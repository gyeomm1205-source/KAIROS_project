"""
quiz service — post /internal/quizzes

execution flow:
    run_quiz(request)
        │
        ├─ 1. build_generation_context()    ← compile target_skill, level, hint into prompt vars
        ├─ 2. retrieve_reference_context()  ← qdrant search for relevant chunks (real client)
        ├─ 3. select_question_types()       ← decide question mix (quiz_type + difficulty)
        ├─ 4. generate_questions()          ← gpt-4o skeleton (TODO: prompt)
        ├─ 5. generate_rubric()             ← gpt-4o skeleton (TODO: prompt)
        └─ 6. _assemble_response()          ← build QuizResponse

ownership rules (ai tech spec v3):
  - this service must not query mysql directly.
  - all domain context arrives via QuizRequest (assembled by spring boot).
  - qdrant is used for retrieval of relevant reference chunks.
  - RecommendationHint (title + reason) is injected by spring boot when quiz
    is triggered from the recommendation screen.
"""

from __future__ import annotations

import logging
from typing import Any

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name, get_prompt_version
from app.core.settings import QDRANT_COLLECTION_NAME
from app.models.schemas import (
    ModelMeta,
    QuestionType,
    QuizQuestion,
    QuizRequest,
    QuizResponse,
    QuizRubric,
    QuizType,
    RecommendationHint,
    ReferenceItem,
    RelativeRank,
    UserContext,
)
from app.services.qdrant_client import get_qdrant_client

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# constants
# ---------------------------------------------------------------------------

# maximum qdrant chunks to fetch as question-generation context
_MAX_CONTEXT_CHUNKS = 5

# question count per quiz type and difficulty
_QUESTION_COUNT: dict[QuizType, dict[RelativeRank, int]] = {
    QuizType.pre_assessment: {RelativeRank.low: 3, RelativeRank.mid: 4, RelativeRank.high: 5},
    QuizType.review:         {RelativeRank.low: 3, RelativeRank.mid: 4, RelativeRank.high: 5},
    QuizType.interview:      {RelativeRank.low: 2, RelativeRank.mid: 3, RelativeRank.high: 4},
}

# question type distribution per quiz type (type -> weight)
_QUESTION_TYPE_MIX: dict[QuizType, list[QuestionType]] = {
    QuizType.pre_assessment: [
        QuestionType.multiple_choice,
        QuestionType.multiple_choice,
        QuestionType.short_answer,
    ],
    QuizType.review: [
        QuestionType.short_answer,
        QuestionType.short_answer,
        QuestionType.multiple_choice,
    ],
    QuizType.interview: [
        QuestionType.short_answer,
        QuestionType.short_answer,
        QuestionType.coding,
    ],
}


# ---------------------------------------------------------------------------
# structured output schemas for llm
# ---------------------------------------------------------------------------

class _SingleQuestion(BaseModel):
    """llm output schema for one quiz question."""
    question: str = Field(description="질문 본문. 모호하지 않게 구체적으로 작성.")
    expected_points: list[str] = Field(
        description="이 질문의 정답 핵심 포인트. 2~4개.",
        min_length=1,
        max_length=4,
    )
    options: list[str] | None = Field(
        default=None,
        description="객관식(multiple_choice)인 경우에만 4개의 보기를 작성. 그 외 null.",
    )


class _QuestionsOutput(BaseModel):
    """llm must respond with this exact structure for question generation."""
    questions: list[_SingleQuestion] = Field(
        description="요청된 수만큼 질문을 생성. 각 질문은 앞서 정해진 question_type 순서를 따름."
    )


class _RubricOutput(BaseModel):
    """llm must respond with this exact structure for rubric generation."""
    full_score_criteria: list[str] = Field(
        description="만점 기준. 핵심 개념을 정확히 이해했을 때의 기준. 2~3개.",
        min_length=1,
        max_length=3,
    )
    partial_score_criteria: list[str] = Field(
        default_factory=list,
        description="부분 점수 기준. 이해는 있으나 불완전한 경우. 1~2개.",
        max_length=2,
    )


# ---------------------------------------------------------------------------
# system and user prompts
# ---------------------------------------------------------------------------

_QUESTION_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
개발자의 현재 학습 수준과 추천된 학습 자료를 바탕으로 퀴즈 문항을 생성합니다.

[문항 생성 원칙]
1. target_skill과 직접 연관된 핵심 개념만 다루세요.
2. current_level에 맞춰 난이도를 조절하세요.
   - low: 기초 개념, 용어 정의, 동작 원리
   - mid: 적용 방법, 비교 분석, 트레이드오프
   - high: 설계 판단, 엣지 케이스, 실무 연계
3. recommendation_hint가 있으면 해당 자료의 핵심 내용을 문항에 반영하세요.
4. 반드시 한국어로 작성하세요.
5. 객관식(multiple_choice)은 정답 1개, 오답 3개로 구성하세요.\
"""

_QUESTION_USER_TEMPLATE = """\
[퀴즈 생성 조건]
- 대상 기술: {target_skill}
- 현재 수준: {current_level}
- 퀴즈 유형: {quiz_type}
- 생성할 문항 수: {question_count}개
- 문항 유형 순서: {question_types}

[추천 컨텍스트 (있을 경우 반영)]
{recommendation_hint_text}

[참고 자료 요약]
{reference_context}

위 조건에 맞춰 {question_count}개의 문항을 생성하세요.\
"""

_RUBRIC_SYSTEM_PROMPT = """\
당신은 KAIROS의 채점 기준 설계 전문가입니다.
생성된 퀴즈 문항들을 보고, 채점 기준(rubric)을 작성합니다.

[채점 기준 원칙]
1. 만점 기준은 핵심 개념을 정확히 이해하고 실무 맥락과 연결했을 때로 정의하세요.
2. 부분 점수 기준은 방향은 맞지만 불완전한 이해를 보일 때로 정의하세요.
3. 반드시 한국어로 작성하세요.\
"""

_RUBRIC_USER_TEMPLATE = """\
[대상 기술]: {target_skill}
[퀴즈 유형]: {quiz_type}
[현재 수준]: {current_level}

[생성된 문항 목록]
{questions_summary}

위 문항들에 대한 채점 기준을 작성하세요.\
"""


# ---------------------------------------------------------------------------
# step 1 — build generation context
# ---------------------------------------------------------------------------

def build_generation_context(request: QuizRequest) -> dict[str, Any]:
    """
    compile all prompt variables from the request into a single context dict.
    this dict is passed to both generate_questions() and generate_rubric().
    """
    question_count = _QUESTION_COUNT[request.quiz_type].get(
        request.difficulty, 3
    )
    question_types = _select_question_types(request.quiz_type, question_count)

    hint_text = _fmt_recommendation_hint(request.recommendation_hint)

    return {
        "target_skill":     request.target_skill,
        "current_level":    request.current_level.value,
        "quiz_type":        request.quiz_type.value,
        "question_count":   question_count,
        "question_types":   question_types,
        "hint_text":        hint_text,
        "user":             request.user,
        "time_limit":       request.time_limit_minutes,
    }


def _select_question_types(quiz_type: QuizType, count: int) -> list[QuestionType]:
    """
    determine the question type list for a given quiz_type and count.
    cycles through the type mix until the required count is reached.
    """
    mix = _QUESTION_TYPE_MIX[quiz_type]
    return [mix[i % len(mix)] for i in range(count)]


# ---------------------------------------------------------------------------
# step 2 — retrieve reference context from qdrant
# ---------------------------------------------------------------------------

def retrieve_reference_context(request: QuizRequest) -> list[dict[str, Any]]:
    """
    search qdrant for content chunks relevant to target_skill.
    these chunks provide factual grounding for question generation.

    uses the real qdrant client (app.services.qdrant_client.get_qdrant_client).
    falls back to recent_references from the request if qdrant is unavailable.

    TODO: refine search strategy once qdrant collection is fully populated
      - add metadata filter: {"skill_tags": {"$in": [target_skill]}}
      - consider hybrid retrieval (keyword + dense) for better coverage
      - apply payload filter: {"allowed": true}
    see: ai tech spec v3, §11-4 (qdrant search rules)
    """
    try:
        client = get_qdrant_client()
        results = client.query(
            collection_name=QDRANT_COLLECTION_NAME,
            query_text=f"{request.target_skill} 개념과 사용법",
            limit=_MAX_CONTEXT_CHUNKS,
        )
        chunks = [
            {
                "title":   hit.metadata.get("title", ""),
                "content": hit.document or "",
                "score":   hit.score,
            }
            for hit in results
        ]
        logger.info(
            "[retrieve_reference_context] qdrant returned %d chunks for skill=%s",
            len(chunks), request.target_skill,
        )
        return chunks

    except Exception as exc:
        logger.warning(
            "[retrieve_reference_context] qdrant unavailable (%s) "
            "— falling back to recent_references from request",
            exc,
        )
        # fallback: use the references spring boot already passed in the request
        return [
            {"title": ref.title, "content": f"skill tags: {', '.join(ref.skill_tags)}", "score": 0.0}
            for ref in request.recent_references
        ]


# ---------------------------------------------------------------------------
# step 4 — generate questions  ← the key function
# ---------------------------------------------------------------------------

async def generate_questions(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> list[_SingleQuestion]:
    """
    generate quiz questions using gpt-4o with structured output.

    primary:  gpt-4o via with_structured_output(_QuestionsOutput)
    fallback: placeholder questions (no llm call) on any exception

    ── how to upgrade this prompt ────────────────────────────────────────────
    TODO(prompt): tune _QUESTION_SYSTEM_PROMPT and _QUESTION_USER_TEMPLATE in
    this file. increment PROMPT_VERSIONS[TaskType.QUIZ_GENERATION] in
    model_router.py after each meaningful change so spring boot can track it.

    TODO(prompt): for interview type, add a "면접관 페르소나" instruction block
    to the system prompt to increase answer depth expectations.
    ──────────────────────────────────────────────────────────────────────────
    """
    try:
        return await _generate_questions_with_llm(context, reference_chunks)
    except Exception as exc:
        logger.warning(
            "[generate_questions] llm call failed (%s) — using placeholder fallback",
            exc,
        )
        return _generate_questions_placeholder(context)


async def _generate_questions_with_llm(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> list[_SingleQuestion]:
    """call gpt-4o with structured output to generate questions."""
    llm = get_model(TaskType.QUIZ_GENERATION, temperature=0.4)
    structured_llm = llm.with_structured_output(_QuestionsOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _QUESTION_SYSTEM_PROMPT),
        ("human",  _QUESTION_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _QuestionsOutput = await chain.ainvoke({
        "target_skill":          context["target_skill"],
        "current_level":         context["current_level"],
        "quiz_type":             context["quiz_type"],
        "question_count":        context["question_count"],
        "question_types":        _fmt_question_types(context["question_types"]),
        "recommendation_hint_text": context["hint_text"],
        "reference_context":     _fmt_reference_chunks(reference_chunks),
    })

    logger.debug(
        "[_generate_questions_with_llm] model=%s prompt_version=%s questions=%d",
        get_model_name(TaskType.QUIZ_GENERATION),
        get_prompt_version(TaskType.QUIZ_GENERATION),
        len(result.questions),
    )
    return result.questions


def _generate_questions_placeholder(context: dict[str, Any]) -> list[_SingleQuestion]:
    """
    rule-based placeholder — no llm call.
    used when api key is not set or llm call fails.
    returns generic questions keyed to target_skill and quiz_type.
    """
    skill = context["target_skill"]
    level = context["current_level"]
    quiz_type = context["quiz_type"]
    count = context["question_count"]
    types = context["question_types"]

    placeholder_templates = {
        QuestionType.short_answer.value: (
            f"{skill}의 핵심 동작 원리를 설명하세요. (수준: {level})",
            [f"{skill}의 주요 개념 설명", "실제 동작 흐름 포함", "예시 코드 또는 사례 언급"],
        ),
        QuestionType.multiple_choice.value: (
            f"{skill}에 대한 설명으로 올바른 것은?",
            [f"{skill}의 정확한 정의 선택"],
        ),
        QuestionType.coding.value: (
            f"{skill}을 활용한 간단한 예제 코드를 작성하세요.",
            ["문법 정확성", "핵심 api 사용", "예외 처리 포함"],
        ),
    }

    questions: list[_SingleQuestion] = []
    for i, q_type in enumerate(types[:count]):
        body, points = placeholder_templates.get(
            q_type.value,
            (f"{skill} 관련 질문 {i + 1}", ["핵심 개념 설명"]),
        )
        options = None
        if q_type == QuestionType.multiple_choice:
            options = [
                f"[보기 1] {skill}의 정확한 설명",
                "[보기 2] 오답 보기",
                "[보기 3] 오답 보기",
                "[보기 4] 오답 보기",
            ]
        questions.append(_SingleQuestion(
            question=f"[{quiz_type} / placeholder] {body}",
            expected_points=points,
            options=options,
        ))
    return questions


# ---------------------------------------------------------------------------
# step 5 — generate rubric  ← key function
# ---------------------------------------------------------------------------

async def generate_rubric(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
) -> _RubricOutput:
    """
    generate a rubric for the quiz using gpt-4o with structured output.

    primary:  gpt-4o via with_structured_output(_RubricOutput)
    fallback: rule-based rubric on any exception

    TODO(prompt): tune _RUBRIC_SYSTEM_PROMPT and _RUBRIC_USER_TEMPLATE in
    this file to produce more context-specific criteria.
    """
    try:
        return await _generate_rubric_with_llm(context, questions)
    except Exception as exc:
        logger.warning(
            "[generate_rubric] llm call failed (%s) — using rule-based fallback",
            exc,
        )
        return _generate_rubric_fallback(context)


async def _generate_rubric_with_llm(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
) -> _RubricOutput:
    """call gpt-4o with structured output to generate rubric."""
    llm = get_model(TaskType.QUIZ_GENERATION, temperature=0.2)
    structured_llm = llm.with_structured_output(_RubricOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _RUBRIC_SYSTEM_PROMPT),
        ("human",  _RUBRIC_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _RubricOutput = await chain.ainvoke({
        "target_skill":      context["target_skill"],
        "quiz_type":         context["quiz_type"],
        "current_level":     context["current_level"],
        "questions_summary": _fmt_questions_for_rubric(questions),
    })
    return result


def _generate_rubric_fallback(context: dict[str, Any]) -> _RubricOutput:
    """rule-based fallback rubric — no llm call."""
    skill = context["target_skill"]
    return _RubricOutput(
        full_score_criteria=[
            f"{skill}의 핵심 개념을 정확히 설명하고 실무 맥락과 연결",
            "예시 또는 코드를 들어 구체적으로 설명",
        ],
        partial_score_criteria=[
            f"{skill}의 방향은 맞으나 세부 설명이 불완전",
        ],
    )


# ---------------------------------------------------------------------------
# step 6 — assemble response
# ---------------------------------------------------------------------------

def _assemble_response(
    request: QuizRequest,
    questions: list[_SingleQuestion],
    rubric: _RubricOutput,
) -> QuizResponse:
    """build the final QuizResponse from generated questions and rubric."""
    quiz_questions = [
        QuizQuestion(
            id=idx + 1,
            type=context_type,
            question=q.question,
            expected_points=q.expected_points,
            options=q.options,
        )
        for idx, (q, context_type) in enumerate(
            zip(
                questions,
                _select_question_types(request.quiz_type, len(questions)),
            )
        )
    ]

    return QuizResponse(
        quiz_type=request.quiz_type,
        questions=quiz_questions,
        rubric=QuizRubric(
            full_score_criteria=rubric.full_score_criteria,
            partial_score_criteria=rubric.partial_score_criteria,
        ),
        references=request.recent_references,
        model_meta=ModelMeta(
            model=get_model_name(TaskType.QUIZ_GENERATION),
            prompt_version=get_prompt_version(TaskType.QUIZ_GENERATION),
            policy_version=request.policy_version,
        ),
    )


# ---------------------------------------------------------------------------
# public entry point
# ---------------------------------------------------------------------------

async def run_quiz(request: QuizRequest) -> QuizResponse:
    """
    main entry point for post /internal/quizzes.

    orchestrates all steps and returns a fully populated QuizResponse.
    """
    logger.info(
        "run_quiz  request_id=%s  user_id=%s  skill=%s  type=%s  level=%s",
        request.request_id,
        request.user.user_id,
        request.target_skill,
        request.quiz_type.value,
        request.current_level.value,
    )

    # 1. compile prompt variables
    context = build_generation_context(request)

    # 2. retrieve grounding context from qdrant
    reference_chunks = retrieve_reference_context(request)

    # 3. generate questions (llm / fallback)
    questions = await generate_questions(context, reference_chunks)

    # 4. generate rubric (llm / fallback)
    rubric = await generate_rubric(context, questions)

    # 5. assemble and return
    response = _assemble_response(request, questions, rubric)

    logger.info(
        "run_quiz done  request_id=%s  questions=%d",
        request.request_id,
        len(response.questions),
    )
    return response


# ---------------------------------------------------------------------------
# internal format helpers
# ---------------------------------------------------------------------------

def _fmt_recommendation_hint(hint: RecommendationHint | None) -> str:
    if hint is None:
        return "없음 (직접 퀴즈 생성 요청)"
    return (
        f"추천 자료 제목: {hint.title}\n"
        f"추천 이유: {hint.reason}"
    )


def _fmt_reference_chunks(chunks: list[dict[str, Any]]) -> str:
    if not chunks:
        return "참고 자료 없음"
    return "\n\n".join(
        f"[{i + 1}] {c.get('title', '제목 없음')}\n{c.get('content', '')[:300]}"
        for i, c in enumerate(chunks)
    )


def _fmt_question_types(types: list[QuestionType]) -> str:
    labels = {
        QuestionType.short_answer:    "주관식(단답)",
        QuestionType.multiple_choice: "객관식(4지선다)",
        QuestionType.coding:          "코딩",
    }
    return ", ".join(f"{i + 1}번: {labels[t]}" for i, t in enumerate(types))


def _fmt_questions_for_rubric(questions: list[_SingleQuestion]) -> str:
    return "\n".join(
        f"{i + 1}. {q.question}"
        for i, q in enumerate(questions)
    )

"""
quiz service — post /internal/quizzes
issue: s14p21a506-121

execution flow:
    run_quiz(request)
        │
        ├─ 1. build_generation_context()    ← compile target_skill, level, hint into prompt vars
        ├─ 2. retrieve_reference_context()  ← qdrant search (real client, fallback: recent_references)
        ├─ 3. generate_questions()          ← gpt-4o rag-grounded, personalized, with_structured_output
        ├─ 4. generate_rubric()             ← gpt-4o keyword-based rubric, with_structured_output
        └─ 5. _assemble_response()          ← build QuizResponse (100% schema compliant)

design decisions:
  - questions are grounded exclusively in qdrant chunks (anti-hallucination)
  - recommendation_hint injects "why this document" context for personalization
  - question type mix is dynamic: varies by both quiz_type AND difficulty
  - rubric includes per-question keyword requirements for essay/coding grading
  - all llm calls use with_structured_output — no raw string parsing
  - any llm failure triggers rule-based fallback without raising to the caller
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
)
from app.services.qdrant_client import get_qdrant_client

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# constants
# ---------------------------------------------------------------------------

# maximum qdrant chunks to fetch as question-generation grounding context
_MAX_CONTEXT_CHUNKS = 5

# truncation limit per chunk to stay within token budget
_CHUNK_CONTENT_MAX_CHARS = 600

# question count table: [quiz_type][difficulty] -> int
_QUESTION_COUNT: dict[QuizType, dict[RelativeRank, int]] = {
    QuizType.pre_assessment: {RelativeRank.low: 3, RelativeRank.mid: 4, RelativeRank.high: 5},
    QuizType.review:         {RelativeRank.low: 3, RelativeRank.mid: 4, RelativeRank.high: 5},
    QuizType.interview:      {RelativeRank.low: 2, RelativeRank.mid: 3, RelativeRank.high: 4},
}

# question type distribution table: [quiz_type][difficulty] -> ordered list of QuestionType
# rationale:
#   pre_assessment/low  → mostly mc to diagnose recognition-level knowledge
#   pre_assessment/high → deeper sa to measure conceptual depth
#   review/low          → sa+mc mix for recall + recognition
#   review/high         → sa+coding to test application ability
#   interview/low       → sa only to practice verbal explanation
#   interview/high      → sa+coding to simulate real interview pressure
_QUESTION_TYPE_MIX: dict[QuizType, dict[RelativeRank, list[QuestionType]]] = {
    QuizType.pre_assessment: {
        RelativeRank.low:  [QuestionType.multiple_choice, QuestionType.multiple_choice, QuestionType.short_answer],
        RelativeRank.mid:  [QuestionType.multiple_choice, QuestionType.short_answer,   QuestionType.short_answer],
        RelativeRank.high: [QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.multiple_choice, QuestionType.short_answer],
    },
    QuizType.review: {
        RelativeRank.low:  [QuestionType.short_answer,   QuestionType.multiple_choice, QuestionType.short_answer],
        RelativeRank.mid:  [QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.multiple_choice],
        RelativeRank.high: [QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.coding],
    },
    QuizType.interview: {
        RelativeRank.low:  [QuestionType.short_answer,   QuestionType.short_answer],
        RelativeRank.mid:  [QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.short_answer],
        RelativeRank.high: [QuestionType.short_answer,   QuestionType.coding,         QuestionType.short_answer],
    },
}


# ---------------------------------------------------------------------------
# internal llm output schemas (not exposed to spring boot)
# ---------------------------------------------------------------------------

class _SingleQuestion(BaseModel):
    """llm output schema for one quiz question."""
    question: str = Field(
        description=(
            "질문 본문. 개인화 맥락이 있으면 서두에 '당신이 최근 학습한 {skill} 개념 관련' 같은 문구를 포함. "
            "모호하지 않게 구체적으로 작성."
        )
    )
    expected_points: list[str] = Field(
        description="이 질문의 모범 답안 핵심 포인트. 2~4개. 각 포인트는 완전한 문장으로 작성.",
        min_length=1,
        max_length=4,
    )
    scoring_keywords: list[str] = Field(
        description=(
            "서술형/코딩의 경우, 채점 시 답안에 반드시 등장해야 하는 핵심 기술 용어. 2~5개. "
            "[참고 자료]에서 직접 추출. 객관식의 경우 빈 리스트."
        ),
        default_factory=list,
    )
    options: list[str] | None = Field(
        default=None,
        description="객관식(multiple_choice)인 경우에만 작성. 정답 1개 + 그럴듯한 오답 3개, 총 4개.",
    )
    correct_option_index: int | None = Field(
        default=None,
        description="객관식의 경우 정답 보기의 0-based 인덱스(0~3). 그 외 null.",
    )


class _QuestionsOutput(BaseModel):
    """llm must return this exact structure for question generation."""
    questions: list[_SingleQuestion] = Field(
        description=(
            "question_types에 명시된 순서와 수량을 정확히 지킨 문항 리스트. "
            "각 문항은 반드시 [참고 자료]에 근거해야 함."
        )
    )


class _PerQuestionRubric(BaseModel):
    """per-question grading criteria for the rubric."""
    question_id: int = Field(description="문항 번호 (1-based)")
    required_keywords: list[str] = Field(
        description=(
            "서술형/코딩 문항: 답안에 포함되어야 하는 키워드 목록. [참고 자료]에서 추출. "
            "객관식 문항: 빈 리스트."
        ),
        default_factory=list,
    )
    full_score_condition: str = Field(
        description="이 문항의 만점 조건. 구체적으로 서술."
    )
    partial_score_condition: str | None = Field(
        default=None,
        description="이 문항의 부분 점수 조건. 객관식은 null.",
    )


class _RubricOutput(BaseModel):
    """llm must return this exact structure for rubric generation."""
    full_score_criteria: list[str] = Field(
        description="퀴즈 전체 만점 기준. 2~3개의 전반적인 기준.",
        min_length=1,
        max_length=3,
    )
    partial_score_criteria: list[str] = Field(
        default_factory=list,
        description="퀴즈 전체 부분 점수 기준. 1~2개.",
        max_length=2,
    )
    per_question_rubrics: list[_PerQuestionRubric] = Field(
        description="문항별 세부 채점 기준. 모든 문항에 대해 작성."
    )


# ---------------------------------------------------------------------------
# prompts — question generation
# ---------------------------------------------------------------------------

_QUESTION_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토이자 시니어 개발자 인터뷰어입니다.
개발자의 개인화된 학습 맥락과 실제 참고 자료를 바탕으로 정밀한 퀴즈 문항을 생성합니다.

━━━ 절대 원칙 ━━━
1. [반드시] 오직 아래 [참고 자료] 섹션에 있는 내용만을 근거로 출제하세요.
   자료에 없는 개념을 추론하거나 창작하는 것은 할루시네이션이므로 금지합니다.
   자료가 부족하면 자료 내에서 출제 가능한 범위만 다루고, 부족한 수는 채우지 마세요.

2. [반드시] [학습 맥락]이 제공된 경우, 문항 서두에 아래와 같은 개인화 문구를 자연스럽게 녹이세요.
   예시: "당신이 최근 학습한 OAuth 흐름을 바탕으로, JWT signature가 필요한 이유를 설명하세요."
   단, 문항 내용 자체는 여전히 [참고 자료] 범위 내에서만 출제합니다.

━━━ 난이도 기준 (current_level) ━━━
• low  → 기초: 용어 정의, 개념 설명, 동작 원리 이해
• mid  → 중급: 비교 분석, 적용 방법, 트레이드오프 판단
• high → 심화: 설계 결정, 엣지 케이스 처리, 장애 대응, 실무 최적화

━━━ quiz_type별 문항 성격 ━━━
• pre_assessment : 현재 지식 수준을 진단 (모를 수도 있다고 가정, 판단적 어조 금지)
• review         : 이미 학습한 내용을 재확인 (자료와 직접 연결, "다시 떠올려보세요" 어조)
• interview      : 실무 면접 상황 가정, 근거와 경험을 함께 서술하도록 유도

━━━ 문항 유형별 규칙 ━━━
• multiple_choice : 정답 1개 + 그럴듯하지만 명백히 틀린 오답 3개 (총 4개 보기 필수)
                    correct_option_index 에 정답 보기의 인덱스(0~3)를 반드시 기입
• short_answer    : scoring_keywords에 [참고 자료]에서 추출한 핵심 용어 2~5개 기입
• coding          : 실행 가능한 최소 코드 요구, 사용 언어/프레임워크 명시, 예외 처리 포함 기준 제시

━━━ 공통 규칙 ━━━
• 모든 문항은 한국어로 작성
• 문항 번호는 부여하지 말 것 (id는 시스템이 자동 부여)
• 각 문항은 독립적으로 평가 가능해야 함 (이전 문항 정답에 종속 금지)\
"""

_QUESTION_USER_TEMPLATE = """\
━━━ 학습자 프로필 ━━━
• 학습 중인 기술  : {target_skill}
• 현재 수준      : {current_level}
• 퀴즈 목적      : {quiz_type}
• 목표 포지션    : {target_positions}

━━━ 학습 맥락 — 왜 이 자료를 공부하게 됐는가 ━━━
{recommendation_hint_text}

━━━ 출제 조건 ━━━
• 총 문항 수     : {question_count}개
• 제한 시간      : {time_limit}분
• 문항 유형 순서 (이 순서와 수량을 정확히 지키세요):
{question_types}

━━━ 참고 자료 — 이 내용만을 바탕으로 출제하세요 ━━━
{reference_context}

[주의] 위 [참고 자료]에 없는 내용은 절대 출제하지 마세요.
정확히 {question_count}개의 문항을 생성하세요.\
"""


# ---------------------------------------------------------------------------
# prompts — rubric generation
# ---------------------------------------------------------------------------

_RUBRIC_SYSTEM_PROMPT = """\
당신은 KAIROS의 채점 기준 설계 전문가입니다.
주어진 퀴즈 문항들에 대해 엄격하고 재현 가능한 채점 기준을 설계합니다.

━━━ 채점 기준 설계 원칙 ━━━

[전체 기준 (full_score_criteria / partial_score_criteria)]
• 만점  : 핵심 개념을 정확히 이해 + 실무 맥락과 연결 + 구체적 예시/코드/수치 포함
• 부분  : 방향은 맞으나 세부 설명 불완전 / 핵심 용어를 언급했으나 정의 부정확

[문항별 기준 (per_question_rubrics)]
• 서술형(short_answer)
  - required_keywords: [참고 자료 요약]에서 직접 추출한 핵심 기술 용어 2~5개
  - full_score_condition: 해당 키워드를 모두 포함하고 연결 관계를 설명한 경우
  - partial_score_condition: 키워드 일부 포함 또는 방향은 맞으나 불완전한 경우

• 객관식(multiple_choice)
  - required_keywords: 빈 리스트 []
  - full_score_condition: "정답 보기 선택"으로 단순 기재
  - partial_score_condition: null

• 코딩(coding)
  - required_keywords: 사용해야 하는 핵심 api / 메서드명
  - full_score_condition: 동작 가능 + 핵심 api 사용 + 예외 처리 포함
  - partial_score_condition: 동작하지만 예외 처리 누락 또는 비효율적 구현

━━━ 공통 규칙 ━━━
• 모든 기준은 한국어로 작성
• 채점자가 주관적 판단 없이 기준만 보고 채점할 수 있도록 구체적으로 작성
• required_keywords는 반드시 [참고 자료 요약]에서만 추출 (창작 금지)\
"""

_RUBRIC_USER_TEMPLATE = """\
━━━ 채점 대상 퀴즈 정보 ━━━
• 대상 기술  : {target_skill}
• 퀴즈 유형  : {quiz_type}
• 현재 수준  : {current_level}

━━━ 생성된 문항 목록 (유형 포함) ━━━
{questions_with_types}

━━━ 출제 근거 자료 요약 (키워드 추출 기준) ━━━
{reference_context_summary}

위 문항들에 대한 채점 기준을 설계하세요.
required_keywords는 반드시 [출제 근거 자료 요약] 내용에서만 추출하세요.\
"""


# ---------------------------------------------------------------------------
# step 1 — build generation context
# ---------------------------------------------------------------------------

def build_generation_context(request: QuizRequest) -> dict[str, Any]:
    """
    compile all prompt-injection variables from the request into one dict.
    passed to both generate_questions() and generate_rubric().
    """
    question_count = _QUESTION_COUNT[request.quiz_type].get(request.difficulty, 3)
    question_types = _select_question_types(
        request.quiz_type, request.difficulty, question_count
    )

    return {
        "target_skill":      request.target_skill,
        "current_level":     request.current_level.value,
        "quiz_type":         request.quiz_type.value,
        "question_count":    question_count,
        "question_types":    question_types,
        "hint_text":         _fmt_recommendation_hint(request.recommendation_hint, request.target_skill),
        "target_positions":  ", ".join(request.user.target_positions) or "미지정",
        "time_limit":        request.time_limit_minutes,
    }


def _select_question_types(
    quiz_type: QuizType,
    difficulty: RelativeRank,
    count: int,
) -> list[QuestionType]:
    """
    return an ordered list of question types for the given quiz_type + difficulty.
    cycles through the mix table if count exceeds the defined pattern length.
    """
    mix = _QUESTION_TYPE_MIX[quiz_type][difficulty]
    return [mix[i % len(mix)] for i in range(count)]


# ---------------------------------------------------------------------------
# step 2 — retrieve reference context from qdrant
# ---------------------------------------------------------------------------

def retrieve_reference_context(request: QuizRequest) -> list[dict[str, Any]]:
    """
    search qdrant for content chunks relevant to target_skill.
    chunks serve as the exclusive factual grounding for question generation.

    primary  : qdrant client with query_text composed from target_skill + hint context
    fallback : recent_references passed by spring boot in the request body

    TODO (once collection is populated):
      - add payload filter: must={"skill_tags": {"$in": [target_skill]}, "allowed": true}
      - switch query_text to hint.title if recommendation_hint is present
        (more focused retrieval on the specific recommended document)
      - consider hybrid retrieval (keyword + dense) for better chunk coverage
    see: ai tech spec v3, §11-4 (qdrant search rules)
    """
    query_text = _build_qdrant_query(request)

    try:
        client = get_qdrant_client()
        results = client.query(
            collection_name=QDRANT_COLLECTION_NAME,
            query_text=query_text,
            limit=_MAX_CONTEXT_CHUNKS,
        )
        chunks = [
            {
                "title":   hit.metadata.get("title", ""),
                "content": (hit.document or "")[:_CHUNK_CONTENT_MAX_CHARS],
                "score":   hit.score,
            }
            for hit in results
        ]
        logger.info(
            "[retrieve_reference_context] qdrant returned %d chunks  skill=%s  query=%r",
            len(chunks), request.target_skill, query_text,
        )
        return chunks

    except Exception as exc:
        logger.warning(
            "[retrieve_reference_context] qdrant unavailable (%s) — fallback to recent_references",
            exc,
        )
        return [
            {
                "title":   ref.title,
                "content": f"skill tags: {', '.join(ref.skill_tags)}",
                "score":   0.0,
            }
            for ref in request.recent_references
        ]


def _build_qdrant_query(request: QuizRequest) -> str:
    """
    compose a qdrant query string.
    if recommendation_hint is present, use the document title for focused retrieval.
    otherwise, fall back to a generic skill-based query.
    """
    if request.recommendation_hint:
        return f"{request.recommendation_hint.title} — {request.target_skill} 핵심 개념"
    return f"{request.target_skill} 개념과 사용 방법"


# ---------------------------------------------------------------------------
# step 3 — generate questions  ← key function
# ---------------------------------------------------------------------------

async def generate_questions(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> list[_SingleQuestion]:
    """
    generate rag-grounded, personalized quiz questions via gpt-4o.

    primary  : gpt-4o with_structured_output(_QuestionsOutput)
               — questions are grounded exclusively in qdrant chunks
               — recommendation_hint injects "why this document" personalization
               — question type mix is driven by quiz_type + difficulty
    fallback : rule-based placeholder questions (no llm call)

    to improve output quality, tune the prompts in this file
    (_QUESTION_SYSTEM_PROMPT, _QUESTION_USER_TEMPLATE) and bump
    PROMPT_VERSIONS[TaskType.QUIZ_GENERATION] in core/model_router.py.
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
    """call gpt-4o with_structured_output to generate questions."""
    llm = get_model(TaskType.QUIZ_GENERATION, temperature=0.4)
    structured_llm = llm.with_structured_output(_QuestionsOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _QUESTION_SYSTEM_PROMPT),
        ("human",  _QUESTION_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _QuestionsOutput = await chain.ainvoke({
        "target_skill":             context["target_skill"],
        "current_level":            context["current_level"],
        "quiz_type":                context["quiz_type"],
        "target_positions":         context["target_positions"],
        "question_count":           context["question_count"],
        "question_types":           _fmt_question_types(context["question_types"]),
        "time_limit":               context["time_limit"],
        "recommendation_hint_text": context["hint_text"],
        "reference_context":        _fmt_reference_chunks(reference_chunks),
    })

    logger.debug(
        "[_generate_questions_with_llm] model=%s  prompt_version=%s  questions=%d",
        get_model_name(TaskType.QUIZ_GENERATION),
        get_prompt_version(TaskType.QUIZ_GENERATION),
        len(result.questions),
    )
    return result.questions


def _generate_questions_placeholder(context: dict[str, Any]) -> list[_SingleQuestion]:
    """
    rule-based placeholder — no llm call.
    activated when api key is missing or llm fails.
    """
    skill    = context["target_skill"]
    level    = context["current_level"]
    quiz_type = context["quiz_type"]
    count    = context["question_count"]
    types    = context["question_types"]

    templates: dict[str, tuple[str, list[str], list[str] | None]] = {
        QuestionType.short_answer.value: (
            f"[{quiz_type}/placeholder] {skill}의 핵심 동작 원리를 {level} 수준에서 설명하세요.",
            [f"{skill}의 주요 개념 설명", "실제 동작 흐름 포함", "예시 또는 코드 언급"],
            [],
        ),
        QuestionType.multiple_choice.value: (
            f"[{quiz_type}/placeholder] {skill}에 대한 설명으로 올바른 것은?",
            [f"{skill}의 정확한 정의를 선택"],
            [f"보기 1: {skill}의 올바른 설명", "보기 2: 오답 보기", "보기 3: 오답 보기", "보기 4: 오답 보기"],
        ),
        QuestionType.coding.value: (
            f"[{quiz_type}/placeholder] {skill}을 활용한 최소 동작 예제 코드를 작성하세요.",
            ["문법 정확성", f"{skill} 핵심 api 사용", "예외 처리 포함"],
            [],
        ),
    }

    questions: list[_SingleQuestion] = []
    for i, q_type in enumerate(types[:count]):
        body, points, opts = templates.get(
            q_type.value,
            (f"[placeholder] {skill} 관련 질문 {i + 1}", ["핵심 개념 설명"], None),
        )
        questions.append(_SingleQuestion(
            question=body,
            expected_points=points,
            scoring_keywords=[],
            options=opts if q_type == QuestionType.multiple_choice else None,
            correct_option_index=0 if q_type == QuestionType.multiple_choice else None,
        ))
    return questions


# ---------------------------------------------------------------------------
# step 4 — generate rubric  ← key function
# ---------------------------------------------------------------------------

async def generate_rubric(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
    reference_chunks: list[dict[str, Any]],
) -> _RubricOutput:
    """
    generate keyword-based, per-question rubric via gpt-4o.

    primary  : gpt-4o with_structured_output(_RubricOutput)
               — required_keywords extracted from qdrant chunks (not hallucinated)
               — per_question_rubrics provides granular criteria per question type
    fallback : rule-based rubric (no llm call)

    note: reference_chunks are passed here so the llm can extract
    accurate scoring_keywords from the actual retrieved material.
    """
    try:
        return await _generate_rubric_with_llm(context, questions, reference_chunks)
    except Exception as exc:
        logger.warning(
            "[generate_rubric] llm call failed (%s) — using rule-based fallback",
            exc,
        )
        return _generate_rubric_fallback(context, questions)


async def _generate_rubric_with_llm(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
    reference_chunks: list[dict[str, Any]],
) -> _RubricOutput:
    """call gpt-4o with_structured_output to generate the rubric."""
    llm = get_model(TaskType.QUIZ_GENERATION, temperature=0.2)
    structured_llm = llm.with_structured_output(_RubricOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _RUBRIC_SYSTEM_PROMPT),
        ("human",  _RUBRIC_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _RubricOutput = await chain.ainvoke({
        "target_skill":           context["target_skill"],
        "quiz_type":              context["quiz_type"],
        "current_level":          context["current_level"],
        "questions_with_types":   _fmt_questions_with_types(questions, context["question_types"]),
        "reference_context_summary": _fmt_reference_chunks_summary(reference_chunks),
    })
    logger.debug(
        "[_generate_rubric_with_llm] per_question_rubrics=%d",
        len(result.per_question_rubrics),
    )
    return result


def _generate_rubric_fallback(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
) -> _RubricOutput:
    """rule-based fallback rubric — no llm call."""
    skill = context["target_skill"]

    per_q: list[_PerQuestionRubric] = []
    for i, q in enumerate(questions):
        q_type = context["question_types"][i] if i < len(context["question_types"]) else QuestionType.short_answer
        if q_type == QuestionType.multiple_choice:
            per_q.append(_PerQuestionRubric(
                question_id=i + 1,
                required_keywords=[],
                full_score_condition="정답 보기 선택",
                partial_score_condition=None,
            ))
        elif q_type == QuestionType.coding:
            per_q.append(_PerQuestionRubric(
                question_id=i + 1,
                required_keywords=q.scoring_keywords or [skill],
                full_score_condition=f"{skill} 핵심 api 사용 + 동작 가능 + 예외 처리 포함",
                partial_score_condition="동작하지만 예외 처리 누락 또는 비효율적 구현",
            ))
        else:
            per_q.append(_PerQuestionRubric(
                question_id=i + 1,
                required_keywords=q.scoring_keywords or [skill],
                full_score_condition=f"{skill} 핵심 개념 설명 + 실무 맥락 연결",
                partial_score_condition=f"{skill} 방향은 맞으나 세부 설명 불완전",
            ))

    return _RubricOutput(
        full_score_criteria=[
            f"{skill}의 핵심 개념을 정확히 설명하고 실무 맥락과 연결",
            "구체적인 예시 또는 코드를 들어 설명",
        ],
        partial_score_criteria=[
            f"{skill} 방향은 맞으나 세부 설명이 불완전",
        ],
        per_question_rubrics=per_q,
    )


# ---------------------------------------------------------------------------
# step 5 — assemble response
# ---------------------------------------------------------------------------

def _assemble_response(
    request: QuizRequest,
    questions: list[_SingleQuestion],
    rubric: _RubricOutput,
) -> QuizResponse:
    """
    build the final QuizResponse, 100% compliant with schemas.QuizResponse.

    scoring_keywords from _SingleQuestion are appended to expected_points so
    spring boot can surface them in the ui without schema changes:
      "필수 키워드: keyword1, keyword2, ..."
    """
    question_types = _select_question_types(
        request.quiz_type, request.difficulty, len(questions)
    )

    quiz_questions: list[QuizQuestion] = []
    for idx, (q, q_type) in enumerate(zip(questions, question_types)):
        points = list(q.expected_points)

        # append scoring keywords as the last expected_point for grader visibility
        if q.scoring_keywords and q_type != QuestionType.multiple_choice:
            points.append(f"필수 키워드: {', '.join(q.scoring_keywords)}")

        # for mc: prepend the correct answer index as the first expected_point
        if q_type == QuestionType.multiple_choice and q.correct_option_index is not None:
            points.insert(0, f"정답: 보기 {q.correct_option_index + 1}")

        quiz_questions.append(QuizQuestion(
            id=idx + 1,
            type=q_type,
            question=q.question,
            expected_points=points,
            options=q.options,
        ))

    # flatten per_question_rubrics into full_score_criteria for easy reading
    rubric_full = list(rubric.full_score_criteria)
    for pq in rubric.per_question_rubrics:
        if pq.required_keywords:
            rubric_full.append(
                f"[{pq.question_id}번] 필수 키워드 포함: {', '.join(pq.required_keywords)}"
            )

    return QuizResponse(
        quiz_type=request.quiz_type,
        questions=quiz_questions,
        rubric=QuizRubric(
            full_score_criteria=rubric_full,
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
        "run_quiz start  request_id=%s  user_id=%s  skill=%s  type=%s  level=%s  hint=%s",
        request.request_id,
        request.user.user_id,
        request.target_skill,
        request.quiz_type.value,
        request.current_level.value,
        bool(request.recommendation_hint),
    )

    # 1. compile prompt variables
    context = build_generation_context(request)

    # 2. retrieve rag grounding context from qdrant
    reference_chunks = retrieve_reference_context(request)

    # 3. generate questions — rag-grounded + personalized (llm / fallback)
    questions = await generate_questions(context, reference_chunks)

    # 4. generate rubric — keyword-based per-question criteria (llm / fallback)
    rubric = await generate_rubric(context, questions, reference_chunks)

    # 5. assemble and return (100% schema compliant)
    response = _assemble_response(request, questions, rubric)

    logger.info(
        "run_quiz done  request_id=%s  questions=%d  model=%s",
        request.request_id,
        len(response.questions),
        response.model_meta.model,
    )
    return response


# ---------------------------------------------------------------------------
# format helpers
# ---------------------------------------------------------------------------

def _fmt_recommendation_hint(hint: RecommendationHint | None, skill: str) -> str:
    """
    format the recommendation hint for prompt injection.
    when hint is present, frames the "why this document" context explicitly
    so the llm can generate questions that connect to the user's learning journey.
    """
    if hint is None:
        return "없음 — 사용자가 직접 퀴즈를 요청했습니다."
    return (
        f"이 퀴즈는 아래 추천 자료를 학습한 직후 점검용으로 생성됩니다.\n"
        f"• 추천 자료 제목 : {hint.title}\n"
        f"• 추천 이유      : {hint.reason}\n\n"
        f"→ 문항 서두에 \"당신이 최근 학습한 {skill} 개념과 연결해서...\" 같은 "
        f"맥락 문구를 자연스럽게 포함하세요."
    )


def _fmt_reference_chunks(chunks: list[dict[str, Any]]) -> str:
    """format qdrant chunks as the grounding context for question generation."""
    if not chunks:
        return "참고 자료 없음 — 일반적인 지식을 기반으로 출제하세요."
    parts = []
    for i, c in enumerate(chunks):
        title   = c.get("title", "제목 없음")
        content = c.get("content", "").strip()
        score   = c.get("score", 0.0)
        parts.append(
            f"[자료 {i + 1}] {title}  (관련도 {score:.2f})\n"
            f"{content}"
        )
    return "\n\n".join(parts)


def _fmt_reference_chunks_summary(chunks: list[dict[str, Any]]) -> str:
    """compact format for the rubric prompt — titles + first 200 chars only."""
    if not chunks:
        return "참고 자료 없음"
    return "\n".join(
        f"• {c.get('title', '제목 없음')}: {c.get('content', '')[:200]}"
        for c in chunks
    )


def _fmt_question_types(types: list[QuestionType]) -> str:
    """format the question type sequence as a numbered instruction list."""
    labels = {
        QuestionType.short_answer:    "서술형(단답/논술)",
        QuestionType.multiple_choice: "객관식(4지선다)",
        QuestionType.coding:          "코딩 실습",
    }
    return "\n".join(f"  {i + 1}번 문항: {labels[t]}" for i, t in enumerate(types))


def _fmt_questions_with_types(
    questions: list[_SingleQuestion],
    types: list[QuestionType],
) -> str:
    """format questions with their type labels for the rubric prompt."""
    labels = {
        QuestionType.short_answer:    "서술형",
        QuestionType.multiple_choice: "객관식",
        QuestionType.coding:          "코딩",
    }
    lines = []
    for i, q in enumerate(questions):
        q_type  = types[i] if i < len(types) else QuestionType.short_answer
        label   = labels.get(q_type, "서술형")
        keyword_note = ""
        if q.scoring_keywords:
            keyword_note = f"  [출제 키워드 힌트: {', '.join(q.scoring_keywords)}]"
        lines.append(f"{i + 1}. [{label}] {q.question}{keyword_note}")
    return "\n".join(lines)

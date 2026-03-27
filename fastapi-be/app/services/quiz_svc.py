"""
퀴즈 서비스 — POST /api/v1/ai/quizzes/generate-async
이슈: S14P21A506-121

실행 흐름:
    run_quiz(request)
        │
        ├─ 1. build_generation_context()         ← target_skill, level, hint → 프롬프트 변수 조립
        ├─ 2. retrieve_reference_context()       ← Qdrant 검색 (폴백: recent_references)
        ├─ 3. generate_questions_and_rubric()    ← gpt-4o 단일 호출: 문항 + 채점 기준 동시 생성
        │      폴백 → generate_questions() + generate_rubric() (2-call 순차)
        └─ 4. _assemble_response()               ← QuizResponse 조립 (스키마 100% 준수)

설계 결정:
  - 문항은 Qdrant 청크에만 근거해 출제 (할루시네이션 방지)
  - recommendation_hint로 "왜 이 자료인가" 맥락을 주입해 문항 개인화
  - 문항 유형 구성은 quiz_type과 difficulty에 따라 동적으로 결정
  - 채점 기준에는 서술형/코딩 문항별 필수 키워드가 포함됨
  - 모든 LLM 호출은 with_structured_output 사용 — 문자열 파싱 없음
  - LLM 실패 시 규칙 기반 폴백으로 호출자에게 예외를 올리지 않음
  - combined 단일 호출로 문항+채점 기준 생성 시 순차 latency 제거
"""

from __future__ import annotations

import logging
from typing import Any

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name, get_prompt_version
from app.core.settings import QDRANT_COLLECTION_NAME
from app.models.schemas import (
    QuestionType,
    QuizQuestion,
    QuizRequest,
    QuizResponse,
    UserLevel,
)
from app.services.ingestion.embedder import embed_text
from app.services.qdrant_client import get_qdrant_client

logger = logging.getLogger(__name__)

# 문항 생성 grounding용 Qdrant 청크 최대 개수
_MAX_CONTEXT_CHUNKS = 5

# 토큰 예산 유지를 위한 청크당 최대 글자 수
_CHUNK_CONTENT_MAX_CHARS = 600

# UserLevel → (quiz_type_label, difficulty_label, question_count)
_LEVEL_CONFIG: dict[UserLevel, tuple[str, str, int]] = {
    UserLevel.junior: ("복습", "low",  10),
    UserLevel.mid:    ("복습", "mid",  10),
    UserLevel.senior: ("심화", "high", 10),
}

# 문항 유형 분배: difficulty → QuestionType 순서 리스트
_QUESTION_TYPE_MIX: dict[str, list[QuestionType]] = {
    "low":  [QuestionType.multiple_choice, QuestionType.multiple_choice, QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.multiple_choice, QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.short_answer],
    "mid":  [QuestionType.multiple_choice, QuestionType.short_answer,   QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.short_answer,   QuestionType.short_answer,
             QuestionType.multiple_choice, QuestionType.short_answer],
    "high": [QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.coding,
             QuestionType.short_answer,   QuestionType.multiple_choice,
             QuestionType.short_answer,   QuestionType.short_answer,   QuestionType.coding,
             QuestionType.short_answer,   QuestionType.multiple_choice],
}


# LLM 출력 스키마 (Spring Boot에 노출되지 않는 내부 스키마)

class _SingleQuestion(BaseModel):
    """문항 하나에 대한 LLM 출력 스키마."""
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
    """문항 생성 시 LLM이 반환해야 하는 구조."""
    questions: list[_SingleQuestion] = Field(
        description=(
            "question_types에 명시된 순서와 수량을 정확히 지킨 문항 리스트. "
            "각 문항은 반드시 [참고 자료]에 근거해야 함."
        )
    )


class _PerQuestionRubric(BaseModel):
    """문항별 채점 기준."""
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
    """채점 기준 생성 시 LLM이 반환해야 하는 구조."""
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


# 프롬프트 — 문항 생성

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


# 프롬프트 — 채점 기준 생성

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


# combined 출력 스키마 — 문항 + 채점 기준을 한 번에

class _CombinedOutput(BaseModel):
    """_QuestionsOutput과 _RubricOutput을 병합한 단일 structured output."""
    questions: list[_SingleQuestion] = Field(
        description=(
            "question_types에 명시된 순서와 수량을 정확히 지킨 문항 리스트. "
            "각 문항은 반드시 [참고 자료]에 근거해야 함."
        )
    )
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


# 프롬프트 — 문항 + 채점 기준 combined 생성

_COMBINED_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토이자 채점 기준 설계 전문가입니다.
개발자의 학습 맥락과 참고 자료를 바탕으로 퀴즈 문항과 채점 기준을 한 번에 생성합니다.

━━━ [파트 1] 문항 생성 원칙 ━━━
1. 오직 [참고 자료] 내용만을 근거로 출제하세요. 자료에 없는 내용은 창작하지 마세요.
2. [학습 맥락]이 제공된 경우, 문항 서두에 개인화 문구를 자연스럽게 포함하세요.
   예: "당신이 최근 학습한 OAuth 흐름을 바탕으로, JWT signature가 필요한 이유를 설명하세요."

━━━ 난이도 기준 ━━━
• low  → 기초: 용어 정의, 개념 설명
• mid  → 중급: 비교 분석, 적용 방법, 트레이드오프
• high → 심화: 설계 결정, 엣지 케이스, 실무 최적화

━━━ quiz_type별 성격 ━━━
• pre_assessment : 현재 지식 수준 진단 (판단적 어조 금지)
• review         : 학습 내용 재확인 ("다시 떠올려보세요" 어조)
• interview      : 실무 면접 상황, 근거와 경험 서술 유도

━━━ 문항 유형 규칙 ━━━
• multiple_choice : 정답 1개 + 오답 3개, correct_option_index 필수
• short_answer    : scoring_keywords에 참고 자료 핵심 용어 2~5개
• coding          : 실행 가능한 최소 코드, 언어/프레임워크 명시

━━━ [파트 2] 채점 기준 설계 원칙 ━━━
• full_score_criteria    : 핵심 개념 정확히 이해 + 실무 맥락 연결 + 구체적 예시 포함 (2~3개)
• partial_score_criteria : 방향은 맞으나 설명 불완전 (1~2개)
• per_question_rubrics   : 생성한 모든 문항에 대해 작성
  - short_answer  → required_keywords: 참고 자료에서 추출한 핵심 용어 2~5개
  - multiple_choice → required_keywords: 빈 리스트
  - coding        → required_keywords: 핵심 api/메서드명

━━━ 공통 규칙 ━━━
• 모든 출력은 한국어
• required_keywords는 반드시 [참고 자료]에서만 추출\
"""

_COMBINED_USER_TEMPLATE = """\
━━━ 학습자 프로필 ━━━
• 학습 중인 기술  : {target_skill}
• 현재 수준      : {current_level}
• 퀴즈 목적      : {quiz_type}
• 목표 포지션    : {target_positions}

━━━ 학습 맥락 ━━━
{recommendation_hint_text}

━━━ 출제 조건 ━━━
• 총 문항 수     : {question_count}개
• 제한 시간      : {time_limit}분
• 문항 유형 순서 (이 순서와 수량을 정확히 지키세요):
{question_types}

━━━ 참고 자료 ━━━
{reference_context}

문항 {question_count}개와 채점 기준을 함께 생성하세요.
[참고 자료]에 없는 내용은 절대 출제하지 마세요.\
"""


# 단계 1 — 생성 컨텍스트 조립

def build_generation_context(request: QuizRequest) -> dict[str, Any]:
    """요청에서 프롬프트 주입용 변수를 하나의 dict로 조립한다."""
    quiz_type_label, difficulty, question_count = _LEVEL_CONFIG[request.user_level]
    question_types = _select_question_types(difficulty, question_count)
    target_skill = request.target_tech_stacks[0] if request.target_tech_stacks else "개발"
    all_skills   = ", ".join(request.target_tech_stacks)
    current_node_hint = _build_current_node_hint(request)

    return {
        "target_skill":      target_skill,
        "all_skills":        all_skills,
        "current_level":     difficulty,
        "quiz_type":         quiz_type_label,
        "question_count":    question_count,
        "question_types":    question_types,
        "hint_text":         current_node_hint or f"커리큘럼 ID {request.curriculum_id} 기반 퀴즈입니다.",
        "target_positions":  "개발자",
        "time_limit":        20,
    }


def _select_question_types(difficulty: str, count: int) -> list[QuestionType]:
    """difficulty에 맞는 문항 유형 순서 리스트를 반환한다."""
    mix = _QUESTION_TYPE_MIX.get(difficulty, _QUESTION_TYPE_MIX["mid"])
    return [mix[i % len(mix)] for i in range(count)]


# 단계 2 — Qdrant에서 참고 컨텍스트 조회

def retrieve_reference_context(request: QuizRequest) -> list[dict[str, Any]]:
    """
    targetTechStacks 기반으로 Qdrant 청크를 검색한다.
    청크는 문항 생성의 유일한 사실 근거로 사용된다.

    TODO (컬렉션 구축 후):
      - payload filter 추가: skill_tags 기반 필터링
      - 하이브리드 검색(키워드 + dense) 고려
    """
    query_text = _build_qdrant_query(request)

    try:
        client = get_qdrant_client()
        query_vector = embed_text(query_text)
        results = client.search(
            collection_name=QDRANT_COLLECTION_NAME,
            query_vector=query_vector,
            limit=_MAX_CONTEXT_CHUNKS,
        )
        chunks = [
            {
                "title":   (hit.payload or {}).get("title", ""),
                "content": ((hit.payload or {}).get("text", "") or "")[:_CHUNK_CONTENT_MAX_CHARS],
                "score":   hit.score,
            }
            for hit in results
        ]
        logger.info(
            "[retrieve_reference_context] Qdrant 반환 %d개 청크  query=%r",
            len(chunks), query_text,
        )
        return chunks

    except Exception as exc:
        logger.warning(
            "[retrieve_reference_context] Qdrant 연결 불가 (%s) — 빈 컨텍스트 폴백",
            exc,
        )
        return []


def _build_qdrant_query(request: QuizRequest) -> str:
    """Qdrant query 문자열을 구성한다. targetTechStacks 기반 검색."""
    primary = request.target_tech_stacks[0] if request.target_tech_stacks else "개발"
    all_skills = " ".join(request.target_tech_stacks[:3])
    node_title = request.current_node_title or ""
    node_desc = request.current_node_description or ""
    return f"{all_skills} {primary} {node_title} {node_desc} 개념과 사용 방법".strip()


def _build_current_node_hint(request: QuizRequest) -> str:
    if not request.current_node_title:
        return ""

    date_part = f"{request.current_node_date} " if request.current_node_date else ""
    desc_part = f" / {request.current_node_description}" if request.current_node_description else ""
    return f"현재 학습 노드는 {date_part}{request.current_node_title}{desc_part} 입니다. 이 맥락에 맞춰 퀴즈를 생성하세요."


# 단계 3 — 문항 생성

async def generate_questions(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> list[_SingleQuestion]:
    """
    gpt-4o로 RAG 기반 개인화 문항을 생성한다.

    주 경로: gpt-4o with_structured_output(_QuestionsOutput)
             - Qdrant 청크에만 근거해 출제 (할루시네이션 방지)
             - recommendation_hint로 "왜 이 자료인가" 개인화 주입
             - 문항 유형 구성은 quiz_type + difficulty에 따라 동적 결정
    폴백:    규칙 기반 placeholder 문항 (LLM 호출 없음)

    출력 품질 개선 시 이 파일의 프롬프트(_QUESTION_SYSTEM_PROMPT, _QUESTION_USER_TEMPLATE)를 수정하고
    core/model_router.py의 PROMPT_VERSIONS[TaskType.QUIZ_GENERATION]을 올린다.
    """
    try:
        return await _generate_questions_with_llm(context, reference_chunks)
    except Exception as exc:
        logger.warning(
            "[generate_questions] LLM 호출 실패 (%s) — placeholder 폴백 사용",
            exc,
        )
        return _generate_questions_placeholder(context)


async def _generate_questions_with_llm(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> list[_SingleQuestion]:
    """gpt-4o with_structured_output으로 문항을 생성한다."""
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
        "[_generate_questions_with_llm] model=%s  prompt_version=%s  문항=%d",
        get_model_name(TaskType.QUIZ_GENERATION),
        get_prompt_version(TaskType.QUIZ_GENERATION),
        len(result.questions),
    )
    return result.questions


def _generate_questions_placeholder(context: dict[str, Any]) -> list[_SingleQuestion]:
    """규칙 기반 placeholder — API 키 없거나 LLM 실패 시 활성화됨."""
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


# 단계 4 — 채점 기준 생성

async def generate_rubric(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
    reference_chunks: list[dict[str, Any]],
) -> _RubricOutput:
    """
    gpt-4o로 키워드 기반 문항별 채점 기준을 생성한다.

    주 경로: gpt-4o with_structured_output(_RubricOutput)
             - required_keywords는 Qdrant 청크에서만 추출 (할루시네이션 방지)
             - per_question_rubrics로 문항 유형별 세분화된 기준 제공
    폴백:    규칙 기반 채점 기준 (LLM 호출 없음)

    reference_chunks를 여기서도 전달하는 이유: LLM이 실제 검색된 자료에서
    정확한 scoring_keywords를 추출하게 하기 위함.
    """
    try:
        return await _generate_rubric_with_llm(context, questions, reference_chunks)
    except Exception as exc:
        logger.warning(
            "[generate_rubric] LLM 호출 실패 (%s) — 규칙 기반 폴백 사용",
            exc,
        )
        return _generate_rubric_fallback(context, questions)


async def _generate_rubric_with_llm(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
    reference_chunks: list[dict[str, Any]],
) -> _RubricOutput:
    """gpt-4o with_structured_output으로 채점 기준을 생성한다."""
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
        "[_generate_rubric_with_llm] 문항별 채점 기준=%d",
        len(result.per_question_rubrics),
    )
    return result


async def generate_questions_and_rubric(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> tuple[list[_SingleQuestion], _RubricOutput]:
    """
    문항과 채점 기준을 단일 LLM 호출로 함께 생성한다.

    generate_questions() → generate_rubric() 순차 호출의 latency를 제거한다.
    combined 호출 실패 시 2-call 순차 경로로 폴백한다.
    """
    try:
        return await _generate_combined_with_llm(context, reference_chunks)
    except Exception as exc:
        logger.warning(
            "[generate_questions_and_rubric] combined 호출 실패 (%s) — 2-call 경로 폴백",
            exc,
        )
        questions = await generate_questions(context, reference_chunks)
        rubric = await generate_rubric(context, questions, reference_chunks)
        return questions, rubric


async def _generate_combined_with_llm(
    context: dict[str, Any],
    reference_chunks: list[dict[str, Any]],
) -> tuple[list[_SingleQuestion], _RubricOutput]:
    """문항과 채점 기준을 gpt-4o 단일 호출로 생성한다."""
    llm = get_model(TaskType.QUIZ_GENERATION, temperature=0.4)
    structured_llm = llm.with_structured_output(_CombinedOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _COMBINED_SYSTEM_PROMPT),
        ("human",  _COMBINED_USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    result: _CombinedOutput = await chain.ainvoke({
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

    rubric = _RubricOutput(
        full_score_criteria=result.full_score_criteria,
        partial_score_criteria=result.partial_score_criteria,
        per_question_rubrics=result.per_question_rubrics,
    )

    logger.debug(
        "[_generate_combined_with_llm] 문항=%d  만점 기준=%d",
        len(result.questions),
        len(result.full_score_criteria),
    )
    return result.questions, rubric


def _generate_rubric_fallback(
    context: dict[str, Any],
    questions: list[_SingleQuestion],
) -> _RubricOutput:
    """규칙 기반 채점 기준 폴백 — LLM 호출 없음."""
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


# 단계 5 — 응답 조립

def _assemble_response(
    request: QuizRequest,
    questions: list[_SingleQuestion],
    rubric: _RubricOutput,
) -> QuizResponse:
    """
    API 명세서 기준 QuizResponse를 조립한다.

    - questionNumber: 1-based 번호
    - quizType: "MULTIPLE_CHOICE" | "SHORT_ANSWER" | "CODING"
    - correctAnswer: 객관식은 정답 텍스트, 나머지는 None
      (Spring Boot가 Redis에 저장, 클라이언트에는 노출하지 않음)
    """
    _, difficulty, _ = _LEVEL_CONFIG[request.user_level]
    question_types = _select_question_types(difficulty, len(questions))

    quiz_questions: list[QuizQuestion] = []
    for idx, (q, q_type) in enumerate(zip(questions, question_types)):
        # 객관식: options[correct_option_index] 텍스트를 correctAnswer로 변환
        correct_answer: str | None = None
        if q_type == QuestionType.multiple_choice and q.options and q.correct_option_index is not None:
            try:
                correct_answer = q.options[q.correct_option_index]
            except IndexError:
                correct_answer = None

        quiz_questions.append(QuizQuestion(
            question_number=idx + 1,
            question=q.question,
            quiz_type=q_type.value,
            options=q.options if q_type == QuestionType.multiple_choice else None,
            correct_answer=correct_answer,
        ))

    target_skill = request.target_tech_stacks[0] if request.target_tech_stacks else "개발"
    quiz_type_label, _, question_count = _LEVEL_CONFIG[request.user_level]

    return QuizResponse(
        curriculum_id=request.curriculum_id,
        total_questions=len(quiz_questions),
        title=f"{target_skill} {quiz_type_label} 퀴즈",
        description=f"{target_skill} 학습 내용 기반 {quiz_type_label} 퀴즈",
        expected_minutes=question_count * 2,
        questions=quiz_questions,
    )


# 공개 엔트리 포인트

async def run_quiz(request: QuizRequest) -> QuizResponse:
    """POST /api/v1/ai/quizzes/generate-async 메인 엔트리 포인트."""
    logger.info(
        "run_quiz 시작  curriculum_id=%s  stacks=%s  level=%s",
        request.curriculum_id,
        request.target_tech_stacks,
        request.user_level.value,
    )

    # 1. 프롬프트 변수 조립
    context = build_generation_context(request)

    # 2. Qdrant에서 RAG grounding 컨텍스트 조회
    reference_chunks = retrieve_reference_context(request)

    # 3. 문항 + 채점 기준 단일 LLM 호출 (폴백: 2-call 순차)
    questions, rubric = await generate_questions_and_rubric(context, reference_chunks)

    # 4. 응답 조립 및 반환
    response = _assemble_response(request, questions, rubric)

    logger.info(
        "run_quiz 완료  curriculum_id=%s  문항=%d",
        request.curriculum_id,
        len(response.questions),
    )
    return response


# 포매팅 헬퍼


def _fmt_reference_chunks(chunks: list[dict[str, Any]]) -> str:
    """문항 생성용 Qdrant 청크를 grounding 컨텍스트로 포매팅한다."""
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
    """채점 기준 프롬프트용 간결 포맷 — 제목 + 앞 200자만 포함한다."""
    if not chunks:
        return "참고 자료 없음"
    return "\n".join(
        f"• {c.get('title', '제목 없음')}: {c.get('content', '')[:200]}"
        for c in chunks
    )


def _fmt_question_types(types: list[QuestionType]) -> str:
    """문항 유형 순서를 번호가 붙은 지시 목록으로 포매팅한다."""
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
    """채점 기준 프롬프트용으로 문항과 유형 레이블을 함께 포매팅한다."""
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

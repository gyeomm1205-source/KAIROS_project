"""
FastAPI AI 서비스 모델 라우터.

모델 선택 규칙 (AI Tech Design v3, Section 16-3):
  - 추출 / 분류 / 단순 요약  → gpt-4o-mini
  - 추천 이유 / 퀴즈 / 커리큘럼 생성 → gpt-4o
  - 스키마 제약 태스크는 항상 structured output 모드 사용.

모든 모델은 SSAFY GMS 엔드포인트를 통해 OpenAI로 프록시된다.
"""

import os
from enum import Enum

from langchain.chat_models import init_chat_model
from langchain_core.language_models import BaseChatModel


# GMS 엔드포인트 설정

GMS_API_KEY: str = os.environ.get("OPENAI_API_KEY", "")
GMS_BASE_URL: str = os.environ.get(
    "OPENAI_API_BASE",
    "https://gms.ssafy.io/gmsapi/api.openai.com/v1",
)


# 태스크 타입 — 모델 선택 기준

class TaskType(str, Enum):
    # gpt-4o-mini 태스크 (빠르고 저렴, 구조화 추출에 적합)
    ACTIVITY_EXTRACTION = "activity_extraction"   # /internal/analyze/activities
    CLASSIFICATION = "classification"
    SIMPLE_SUMMARY = "simple_summary"

    # gpt-4o 태스크 (복잡한 생성, 추론 필요)
    RECOMMENDATION_REASON = "recommendation_reason"   # /internal/recommendations
    QUIZ_GENERATION = "quiz_generation"               # /internal/quizzes
    CURRICULUM_GENERATION = "curriculum_generation"   # /internal/curriculums
    QUERY_REWRITING = "query_rewriting"               # RAG 쿼리 생성


# 태스크별 모델 선택 테이블

_TASK_MODEL_MAP: dict[TaskType, str] = {
    # 추출 / 분류
    TaskType.ACTIVITY_EXTRACTION:    "gpt-4o-mini",
    TaskType.CLASSIFICATION:         "gpt-4o-mini",
    TaskType.SIMPLE_SUMMARY:         "gpt-4o-mini",

    # 생성 (깊은 추론 필요)
    TaskType.RECOMMENDATION_REASON:  "gpt-4o",
    TaskType.QUIZ_GENERATION:        "gpt-4o",
    TaskType.CURRICULUM_GENERATION:  "gpt-4o-mini",
    TaskType.QUERY_REWRITING:        "gpt-4o-mini",  # 경량 쿼리 재작성은 mini로 충분
}

# 프롬프트 버전 추적 — 태스크별 프롬프트 변경 시 올릴 것
PROMPT_VERSIONS: dict[TaskType, str] = {
    TaskType.ACTIVITY_EXTRACTION:    "extract_v1",
    TaskType.CLASSIFICATION:         "classify_v1",
    TaskType.SIMPLE_SUMMARY:         "summary_v1",
    TaskType.RECOMMENDATION_REASON:  "recommend_v1",
    TaskType.QUIZ_GENERATION:        "quiz_v1",
    TaskType.CURRICULUM_GENERATION:  "curriculum_v1",
    TaskType.QUERY_REWRITING:        "query_v1",
}


# 공개 인터페이스

def get_model(task: TaskType, temperature: float = 0.2) -> BaseChatModel:
    """
    주어진 태스크에 맞는 LangChain 챗 모델을 반환한다.

    사용 예:
        llm = get_model(TaskType.RECOMMENDATION_REASON)
        result = llm.invoke(...)
    """
    model_name = _TASK_MODEL_MAP[task]
    return init_chat_model(
        model_name,
        model_provider="openai",
        temperature=temperature,
    )


def get_model_name(task: TaskType) -> str:
    """ModelMeta 로깅용 모델 이름 문자열을 반환한다."""
    return _TASK_MODEL_MAP[task]


def get_prompt_version(task: TaskType) -> str:
    """ModelMeta 로깅용 현재 프롬프트 버전 문자열을 반환한다."""
    return PROMPT_VERSIONS[task]

"""
model routing for fastapi ai service.

spec rule (ai tech design v3, section 16-3):
  - extraction / classification / simple summarization -> gpt-4o-mini
  - recommendation reason / quiz / curriculum generation -> gpt-4o
  - schema-constrained tasks always prefer structured output mode.
"""

import os
from enum import Enum

from langchain.chat_models import init_chat_model
from langchain_core.language_models import BaseChatModel


# ---------------------------------------------------------------------------
# gms endpoint configuration
# ---------------------------------------------------------------------------

GMS_API_KEY: str = os.environ.get("OPENAI_API_KEY", "")
GMS_BASE_URL: str = os.environ.get(
    "OPENAI_API_BASE",
    "https://gms.ssafy.io/gmsapi/api.openai.com/v1",
)


# ---------------------------------------------------------------------------
# task types
# ---------------------------------------------------------------------------

class TaskType(str, Enum):
    # gpt-4o-mini (fast, cheap, structured extraction)
    ACTIVITY_EXTRACTION = "activity_extraction"
    CLASSIFICATION = "classification"
    SIMPLE_SUMMARY = "simple_summary"
    QUERY_REWRITING = "query_rewriting"

    # gpt-4o (complex generation, deeper reasoning)
    RECOMMENDATION_REASON = "recommendation_reason"
    QUIZ_GENERATION = "quiz_generation"
    CURRICULUM_GENERATION = "curriculum_generation"


# ---------------------------------------------------------------------------
# routing table
# ---------------------------------------------------------------------------

_TASK_MODEL_MAP: dict[TaskType, str] = {
    TaskType.ACTIVITY_EXTRACTION:   "gpt-4o-mini",
    TaskType.CLASSIFICATION:        "gpt-4o-mini",
    TaskType.SIMPLE_SUMMARY:        "gpt-4o-mini",
    TaskType.QUERY_REWRITING:       "gpt-4o-mini",
    TaskType.RECOMMENDATION_REASON: "gpt-4o",
    TaskType.QUIZ_GENERATION:       "gpt-4o",
    TaskType.CURRICULUM_GENERATION: "gpt-4o",
}

PROMPT_VERSIONS: dict[TaskType, str] = {
    TaskType.ACTIVITY_EXTRACTION:   "extract_v1",
    TaskType.CLASSIFICATION:        "classify_v1",
    TaskType.SIMPLE_SUMMARY:        "summary_v1",
    TaskType.QUERY_REWRITING:       "query_v1",
    TaskType.RECOMMENDATION_REASON: "recommend_v1",
    TaskType.QUIZ_GENERATION:       "quiz_v1",
    TaskType.CURRICULUM_GENERATION: "curriculum_v1",
}


# ---------------------------------------------------------------------------
# public interface
# ---------------------------------------------------------------------------

def get_model(task: TaskType, temperature: float = 0.2) -> BaseChatModel:
    """return a configured langchain chat model for the given task."""
    return init_chat_model(
        _TASK_MODEL_MAP[task],
        model_provider="openai",
        temperature=temperature,
    )


def get_model_name(task: TaskType) -> str:
    return _TASK_MODEL_MAP[task]


def get_prompt_version(task: TaskType) -> str:
    return PROMPT_VERSIONS[task]

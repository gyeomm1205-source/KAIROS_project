"""
Model routing for FastAPI AI service.

Spec rule (AI Tech Design v3, Section 16-3):
  - Extraction / classification / simple summarization  → gpt-4o-mini
  - Recommendation reason / quiz / curriculum generation → gpt-4o (or equivalent)
  - Schema-constrained tasks always prefer structured output mode.

All models are routed through the SSAFY GMS endpoint, which proxies to OpenAI.
"""

import os
from enum import Enum

from langchain.chat_models import init_chat_model
from langchain_core.language_models import BaseChatModel


# ---------------------------------------------------------------------------
# GMS endpoint configuration
# ---------------------------------------------------------------------------

GMS_API_KEY: str = os.environ.get("OPENAI_API_KEY", "")
GMS_BASE_URL: str = os.environ.get(
    "OPENAI_API_BASE",
    "https://gms.ssafy.io/gmsapi/api.openai.com/v1",
)


# ---------------------------------------------------------------------------
# Task types — controls which model is selected
# ---------------------------------------------------------------------------

class TaskType(str, Enum):
    # gpt-4o-mini tasks (fast, cheap, structured extraction)
    ACTIVITY_EXTRACTION = "activity_extraction"   # /internal/analyze/activities
    CLASSIFICATION = "classification"
    SIMPLE_SUMMARY = "simple_summary"

    # gpt-4o tasks (complex generation, reasoning)
    RECOMMENDATION_REASON = "recommendation_reason"   # /internal/recommendations
    QUIZ_GENERATION = "quiz_generation"               # /internal/quizzes
    CURRICULUM_GENERATION = "curriculum_generation"   # /internal/curriculums
    QUERY_REWRITING = "query_rewriting"               # RAG query generation


# ---------------------------------------------------------------------------
# Model selection table
# ---------------------------------------------------------------------------

_TASK_MODEL_MAP: dict[TaskType, str] = {
    # --- extraction / classification ---
    TaskType.ACTIVITY_EXTRACTION:    "gpt-4o-mini",
    TaskType.CLASSIFICATION:         "gpt-4o-mini",
    TaskType.SIMPLE_SUMMARY:         "gpt-4o-mini",

    # --- generation (requires deeper reasoning) ---
    TaskType.RECOMMENDATION_REASON:  "gpt-4o",
    TaskType.QUIZ_GENERATION:        "gpt-4o",
    TaskType.CURRICULUM_GENERATION:  "gpt-4o",
    TaskType.QUERY_REWRITING:        "gpt-4o-mini",  # lightweight rewrite is fine with mini
}

# Prompt version tracking — increment when prompts change for each task
PROMPT_VERSIONS: dict[TaskType, str] = {
    TaskType.ACTIVITY_EXTRACTION:    "extract_v1",
    TaskType.CLASSIFICATION:         "classify_v1",
    TaskType.SIMPLE_SUMMARY:         "summary_v1",
    TaskType.RECOMMENDATION_REASON:  "recommend_v1",
    TaskType.QUIZ_GENERATION:        "quiz_v1",
    TaskType.CURRICULUM_GENERATION:  "curriculum_v1",
    TaskType.QUERY_REWRITING:        "query_v1",
}


# ---------------------------------------------------------------------------
# Public interface
# ---------------------------------------------------------------------------

def get_model(task: TaskType, temperature: float = 0.2) -> BaseChatModel:
    """
    Return a configured LangChain chat model for the given task.

    Usage:
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
    """Return just the model name string (for ModelMeta logging)."""
    return _TASK_MODEL_MAP[task]


def get_prompt_version(task: TaskType) -> str:
    """Return the current prompt version string for this task (for ModelMeta logging)."""
    return PROMPT_VERSIONS[task]

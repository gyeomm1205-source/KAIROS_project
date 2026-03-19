"""
Pydantic schemas for Spring Boot ↔ FastAPI internal API contracts.

v2 — API 명세서 v1.1 기준 (camelCase, /api/v1/ai prefix)
- FastAPI receives a fully assembled context from Spring Boot.
- FastAPI never queries MySQL directly.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


# ---------------------------------------------------------------------------
# Enums
# ---------------------------------------------------------------------------

class Source(str, Enum):
    github = "github"
    velog = "velog"
    calendar = "calendar"


class Category(str, Enum):
    development = "개발"
    learning = "학습"
    other = "기타"


class RelativeRank(str, Enum):
    high = "high"
    mid = "mid"
    low = "low"


class QuestionType(str, Enum):
    short_answer = "SHORT_ANSWER"
    multiple_choice = "MULTIPLE_CHOICE"
    coding = "CODING"


class FreshnessGrade(str, Enum):
    stable = "stable"
    warning = "warning"
    outdated = "outdated"


class NodeType(str, Enum):
    study = "study"
    action = "action"
    review = "review"


class UserLevel(str, Enum):
    junior = "JUNIOR"
    mid = "MID"
    senior = "SENIOR"


class ReferenceType(str, Enum):
    official_docs = "OFFICIAL_DOCS"
    tech_blog = "TECH_BLOG"
    wiki = "WIKI"
    video = "VIDEO"


# ---------------------------------------------------------------------------
# camelCase base (API 명세서 통신용)
# ---------------------------------------------------------------------------

class _CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )


# ---------------------------------------------------------------------------
# Shared sub-models (내부용 — 기존 유지)
# ---------------------------------------------------------------------------

class ModelMeta(BaseModel):
    """LLM 호출 메타데이터."""
    model: str
    prompt_version: str
    policy_version: str = "policy_v1"


class ReferenceItem(BaseModel):
    """Qdrant 검색 레퍼런스 문서 단위."""
    doc_id: str
    title: str
    url: str
    freshness: FreshnessGrade = FreshnessGrade.stable
    skill_tags: list[str] = Field(default_factory=list)
    source_type: str = Field(default="unknown")


class LearningState(BaseModel):
    """학습 상태 — 내부 분석용."""
    skill: str
    raw_score: float = Field(..., ge=0.0)
    relative_rank: RelativeRank
    percentile: int = Field(..., ge=0, le=100)


class RecentActivity(BaseModel):
    """최근 활동 — 내부 분석용."""
    source: Source
    category: Category
    tech_stacks: list[str]
    summary: str
    activity_date: datetime | None = None


class SourceItem(BaseModel):
    """활동 원시 데이터 1건."""
    source: Source
    source_item_id: str
    title: str
    text: str
    metadata: dict[str, Any] = Field(default_factory=dict)


# ---------------------------------------------------------------------------
# 활동 분석 (내부 API — 기존 유지)
# ---------------------------------------------------------------------------

class AnalyzeActivitiesRequest(BaseModel):
    """POST /internal/analyze/activities"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    source_items: list[SourceItem] = Field(..., min_length=1)


class ActivityResult(BaseModel):
    source: Source
    source_item_id: str
    category: Category
    tech_stacks: list[str]
    summary: str
    activity_date: datetime | None = None
    confidence: float = Field(..., ge=0.0, le=1.0)


class AnalysisSummary(BaseModel):
    observed_strengths: list[str]
    observed_gaps: list[str]
    recommended_positions: list[str]


class AnalyzeActivitiesResponse(BaseModel):
    activities: list[ActivityResult]
    analysis_summary: AnalysisSummary
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta


# ---------------------------------------------------------------------------
# 커리큘럼 생성 — POST /api/v1/ai/curriculum/generate
# ---------------------------------------------------------------------------

class GoogleCalendarEvent(_CamelModel):
    """Google Calendar 이벤트."""
    title: str
    start_date: str = Field(..., description="YYYY-MM-DD")
    end_date: str = Field(..., description="YYYY-MM-DD")


class ManualGeneration(_CamelModel):
    """수동 커리큘럼 생성 파라미터."""
    is_manual: bool
    topic: str | None = None
    goal_type: str | None = None
    specific_goal: str | None = None
    survey_answers: dict[str, str] | None = None


class CurriculumRequest(_CamelModel):
    """POST /api/v1/ai/curriculum/generate 요청."""
    user_id: int
    consider_personal_schedule: bool = False
    google_calendar_events: list[GoogleCalendarEvent] = Field(default_factory=list)
    profile_data: dict[str, Any] | None = None
    manual_generation: ManualGeneration | None = None


class CurriculumRecommendationReason(_CamelModel):
    """커리큘럼 추천 이유 (4개 필드)."""
    summary_line: str
    user_context: str
    ai_interpretation: str
    curriculum_rationale: str


class CurriculumNode(_CamelModel):
    """커리큘럼 학습 노드 1건."""
    title: str
    description: str | None = None
    scheduled_date: str = Field(..., description="YYYY-MM-DD")
    expected_minutes: int


class CurriculumResponse(_CamelModel):
    """POST /api/v1/ai/curriculum/generate 응답."""
    recommendation_reason: CurriculumRecommendationReason
    nodes: list[CurriculumNode]


# ---------------------------------------------------------------------------
# 퀴즈 생성 — POST /api/v1/ai/quizzes/generate-async
# ---------------------------------------------------------------------------

class QuizRequest(_CamelModel):
    """POST /api/v1/ai/quizzes/generate-async 요청."""
    curriculum_id: int
    target_tech_stacks: list[str] = Field(..., min_length=1)
    user_level: UserLevel


class QuizQuestion(_CamelModel):
    """퀴즈 문항 1건."""
    question_number: int
    question: str
    quiz_type: str  # "MULTIPLE_CHOICE" | "SHORT_ANSWER" | "CODING"
    options: list[str] | None = None
    correct_answer: str | None = None  # Spring Boot가 Redis에 저장, 클라이언트 비노출


class QuizResponse(_CamelModel):
    """POST /api/v1/ai/quizzes/generate-async 응답."""
    curriculum_id: int
    total_questions: int
    questions: list[QuizQuestion]


# ---------------------------------------------------------------------------
# 추천 생성 — POST /api/v1/ai/recommendations/generate
# ---------------------------------------------------------------------------

class RecommendationRequest(_CamelModel):
    """POST /api/v1/ai/recommendations/generate 요청."""
    user_id: int
    current_level: UserLevel
    recent_curricula_ids: list[int] = Field(default_factory=list)
    favorite_tech_stacks: list[str] = Field(default_factory=list)


class RecommendationReason(_CamelModel):
    """추천 이유."""
    summary: str
    detail: str


class RecommendationNextNode(_CamelModel):
    """추천 학습 경로 노드."""
    title: str


class RecommendationReference(_CamelModel):
    """추천 레퍼런스 1건."""
    title: str
    recommendation_reason: str
    reference_type: str  # "OFFICIAL_DOCS" | "TECH_BLOG" | "WIKI" | "VIDEO"
    published_at: str | None = None
    url: str


class RecommendationResponse(_CamelModel):
    """POST /api/v1/ai/recommendations/generate 응답."""
    user_id: int
    recommendation_reason: RecommendationReason
    next_nodes: list[RecommendationNextNode]
    references: list[RecommendationReference]


# ---------------------------------------------------------------------------
# 폴백 / 에러 봉투
# ---------------------------------------------------------------------------

class MinimalFallbackResponse(BaseModel):
    """LLM 구조화 실패 시 최소 보장 응답."""
    error: str
    fallback: bool = True
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta | None = None

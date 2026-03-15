"""
pydantic schemas for spring boot <-> fastapi internal api contracts.

design principles (ai tech spec v3):
- fastapi receives a fully assembled aicontext from spring boot.
- fastapi never queries mysql directly.
- all responses include model_meta, references, and assumptions/warnings.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# enums
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


class QuizType(str, Enum):
    pre_assessment = "pre_assessment"   # 사전 평가
    review = "review"                   # 복습
    interview = "interview"             # 면접 대비


class QuestionType(str, Enum):
    short_answer = "short_answer"
    multiple_choice = "multiple_choice"
    coding = "coding"


class RecommendationRequestType(str, Enum):
    reference = "reference"
    activity = "activity"


class FreshnessGrade(str, Enum):
    stable = "stable"
    warning = "warning"
    outdated = "outdated"


class NodeType(str, Enum):
    study = "study"
    action = "action"
    review = "review"


# ---------------------------------------------------------------------------
# shared sub-models
# ---------------------------------------------------------------------------

class ModelMeta(BaseModel):
    """llm call metadata — stored by spring boot for tracing."""
    model: str = Field(..., examples=["gpt-4o-mini", "gpt-4o"])
    prompt_version: str = Field(..., examples=["extract_v2", "quiz_v1"])
    policy_version: str = Field(default="policy_v1")


class ReferenceItem(BaseModel):
    """single reference document retrieved from qdrant."""
    doc_id: str = Field(..., examples=["ref_jwt_001"])
    title: str
    url: str = Field(..., examples=["https://example.com/jwt-bcp"])
    freshness: FreshnessGrade = FreshnessGrade.stable
    skill_tags: list[str] = Field(default_factory=list)
    source_type: str = Field(default="unknown")


# ---------------------------------------------------------------------------
# aicontext — input from spring boot
# ---------------------------------------------------------------------------

class SurveyContext(BaseModel):
    job_role: str
    target_positions: list[str]
    tech_stacks: list[str]
    persona: str = Field(default="default")


class UserContext(BaseModel):
    user_id: int
    job_role: str
    target_positions: list[str] = Field(default_factory=list)
    survey_context: SurveyContext | None = None


class LearningState(BaseModel):
    """skill scoring result calculated by spring boot."""
    skill: str
    raw_score: float = Field(..., ge=0.0)
    relative_rank: RelativeRank
    percentile: int = Field(..., ge=0, le=100)


class RecentActivity(BaseModel):
    source: Source
    category: Category
    tech_stacks: list[str]
    summary: str
    activity_date: datetime | None = None


class CalendarConstraint(BaseModel):
    date: str = Field(..., examples=["2026-03-10"])
    busy_slots: list[str] = Field(default_factory=list)


class SourceItem(BaseModel):
    """raw text item fetched by spring boot from github/velog/calendar."""
    source: Source
    source_item_id: str
    title: str
    text: str
    metadata: dict[str, Any] = Field(default_factory=dict)


# ---------------------------------------------------------------------------
# request bodies — sent by spring boot to fastapi
# ---------------------------------------------------------------------------

class AnalyzeActivitiesRequest(BaseModel):
    """post /internal/analyze/activities"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    source_items: list[SourceItem] = Field(..., min_length=1)


class RecommendationRequest(BaseModel):
    """post /internal/recommendations"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    learning_states: list[LearningState]
    recent_activities: list[RecentActivity]
    history_exclusions: list[str] = Field(default_factory=list)
    calendar_constraints: list[CalendarConstraint] = Field(default_factory=list)
    request_type: RecommendationRequestType = RecommendationRequestType.reference


class RecommendationHint(BaseModel):
    """
    optional context passed by spring boot when a quiz is triggered
    directly from a recommendation result.

    spring boot picks one RecommendationItem and forwards its title + reason
    so the quiz generator can tailor questions to the specific material.
    """
    title: str = Field(..., description="추천된 레퍼런스 제목")
    reason: str = Field(..., description="해당 자료를 추천한 이유 (llm이 생성한 문장)")
    doc_id: str | None = Field(default=None, description="연결된 qdrant doc_id")


class QuizRequest(BaseModel):
    """post /internal/quizzes"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    target_skill: str = Field(..., examples=["JWT"])
    current_level: RelativeRank
    recent_references: list[ReferenceItem] = Field(default_factory=list)
    quiz_type: QuizType = QuizType.review
    difficulty: RelativeRank = RelativeRank.mid
    time_limit_minutes: int = Field(default=20, ge=5, le=120)
    recommendation_hint: RecommendationHint | None = Field(
        default=None,
        description="추천 화면에서 퀴즈를 생성할 때 spring boot가 전달하는 추천 아이템 컨텍스트",
    )


class CurriculumRequest(BaseModel):
    """post /internal/curriculums"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    goals: list[str] = Field(..., min_length=1)
    learning_states: list[LearningState]
    calendar_constraints: list[CalendarConstraint]
    accepted_references: list[ReferenceItem] = Field(default_factory=list)
    duration_days: int = Field(default=5, ge=1, le=30)


# ---------------------------------------------------------------------------
# response schemas — returned by fastapi to spring boot
# ---------------------------------------------------------------------------

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


class RecommendationItem(BaseModel):
    type: RecommendationRequestType
    title: str
    reason: str
    difficulty: RelativeRank
    estimated_time: str
    doc_id: str | None = None


class RecommendationResponse(BaseModel):
    items: list[RecommendationItem]
    reason: str
    assumptions: list[str] = Field(default_factory=list)
    references: list[ReferenceItem]
    outdated_warning: str | None = None
    model_meta: ModelMeta


class QuizQuestion(BaseModel):
    id: int
    type: QuestionType
    question: str
    expected_points: list[str] = Field(default_factory=list)
    options: list[str] | None = Field(default=None)


class QuizRubric(BaseModel):
    full_score_criteria: list[str]
    partial_score_criteria: list[str] = Field(default_factory=list)


class QuizResponse(BaseModel):
    """post /internal/quizzes response."""
    quiz_type: QuizType
    questions: list[QuizQuestion]
    rubric: QuizRubric
    references: list[ReferenceItem] = Field(default_factory=list)
    model_meta: ModelMeta


class CurriculumNode(BaseModel):
    day: int = Field(..., ge=1)
    title: str
    type: NodeType
    estimated_time: str
    description: str | None = None
    reference_doc_ids: list[str] = Field(default_factory=list)


class CurriculumResponse(BaseModel):
    duration_days: int
    why: str
    assumptions: list[str] = Field(default_factory=list)
    nodes: list[CurriculumNode]
    references: list[ReferenceItem] = Field(default_factory=list)
    model_meta: ModelMeta


# ---------------------------------------------------------------------------
# fallback envelope
# ---------------------------------------------------------------------------

class MinimalFallbackResponse(BaseModel):
    """minimum guaranteed response when llm structured output fails (spec §14)."""
    error: str
    fallback: bool = True
    references: list[ReferenceItem] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta | None = None

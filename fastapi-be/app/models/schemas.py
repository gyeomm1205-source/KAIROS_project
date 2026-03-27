"""
Pydantic schemas for Spring Boot ↔ FastAPI internal API contracts.

v3 — 커리큘럼 3분기(ONBOARDING/AUTO/MANUAL) + DailyRecommendation + GrowthSummary
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
    job_preparation = "취준"
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


class CurriculumType(str, Enum):
    """커리큘럼 생성 분기 타입."""
    onboarding = "ONBOARDING"  # 최초 가입 직후 — profile_analyzer 결과 + user_tech_stack + 캘린더
    auto = "AUTO"               # 이후 자동 생성 — activity_history + skill_stats + 캘린더
    manual = "MANUAL"           # 사용자 직접 주제 지정


# ---------------------------------------------------------------------------
# camelCase base (API 명세서 통신용)
# ---------------------------------------------------------------------------

class _CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )


# ---------------------------------------------------------------------------
# Shared sub-models (내부용)
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
# 활동 분석 (내부 API)
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
# 공통 서브 모델
# ---------------------------------------------------------------------------

class GoogleCalendarEvent(_CamelModel):
    """Google Calendar 바쁜 일정."""
    title: str
    start_date: str = Field(..., description="YYYY-MM-DD")
    end_date: str = Field(..., description="YYYY-MM-DD")


class SkillStat(_CamelModel):
    """기술별 활동 빈도 통계 1건 (SpringBoot DB 집계)."""
    skill: str
    count: int


class ActivityHistoryItem(_CamelModel):
    """SpringBoot activity_history 레코드 1건."""
    activity_type: str           # "COMMIT" | "PR" | "VELOG"
    category: str                # "개발" | "학습" | "취준" | "기타"
    title: str
    description: str | None = None
    activity_date: str           # ISO datetime string
    tech_stacks: list[str] = Field(default_factory=list)


# ---------------------------------------------------------------------------
# 커리큘럼 생성 — POST /api/v1/ai/curriculum/generate
# ---------------------------------------------------------------------------

# ONBOARDING 전용 — profile_analyzer 결과 구조 (api-spec POST /curricula/preview 명세 기준)
class TechDetail(_CamelModel):
    """profile_analyzer가 추출한 기술 상세."""
    tech_name: str
    proficiency_percentage: int = Field(ge=0, le=100)
    usage_count: int = 0
    position_name: str | None = None
    fit_level: str | None = None  # "HIGH" | "MID" | "LOW"


class RecommendedPosition(_CamelModel):
    """profile_analyzer가 추천한 포지션."""
    position_name: str
    fit_level: str  # "HIGH" | "MID" | "LOW"


class AnalysisData(_CamelModel):
    """브라우저 Cache에 저장된 profile_analyzer 결과.
    FE → SpringBoot → FastAPI로 전달됨 (api-spec POST /curricula/preview 기준).
    """
    summary: str
    tech_details: list[TechDetail] = Field(default_factory=list)
    recommended_positions: list[RecommendedPosition] = Field(default_factory=list)


# MANUAL 전용
class ManualGeneration(_CamelModel):
    """사용자 직접 지정 커리큘럼 파라미터."""
    is_manual: bool
    topic: str | None = None
    goal_type: str | None = None
    specific_goal: str | None = None
    survey_answers: dict[str, str] | None = None


class CurriculumRequest(_CamelModel):
    """POST /api/v1/ai/curriculum/generate 요청 (3분기 통합).

    curriculum_type에 따라 사용되는 필드가 달라진다:
      ONBOARDING: analysis_data + user_tech_stacks + google_calendar_events
      AUTO:       recent_activities + user_tech_stacks + google_calendar_events
      MANUAL:     manual_generation + user_tech_stacks + google_calendar_events
    """
    user_id: int
    curriculum_type: CurriculumType = CurriculumType.onboarding
    consider_personal_schedule: bool = False
    google_calendar_events: list[GoogleCalendarEvent] = Field(default_factory=list)

    # ONBOARDING 전용
    analysis_data: AnalysisData | None = None

    # ONBOARDING + AUTO 공통 — SpringBoot DB의 user_tech_stack 통계
    user_tech_stacks: list[SkillStat] = Field(default_factory=list)

    # AUTO 전용 — 최근 activity_history 레코드
    recent_activities: list[ActivityHistoryItem] = Field(default_factory=list)

    # MANUAL 전용
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
    tech_stacks: list[str] = Field(default_factory=list)
    nodes: list[CurriculumNode]


# ---------------------------------------------------------------------------
# 퀴즈 생성 — POST /api/v1/ai/quizzes/generate-async
# ---------------------------------------------------------------------------

class QuizRequest(_CamelModel):
    """POST /api/v1/ai/quizzes/generate-async 요청."""
    curriculum_id: int
    target_tech_stacks: list[str] = Field(..., min_length=1)
    user_level: UserLevel
    current_node_title: str | None = None
    current_node_description: str | None = None
    current_node_date: str | None = None


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
    title: str
    description: str
    expected_minutes: int
    questions: list[QuizQuestion]


# ---------------------------------------------------------------------------
# 추천 생성 — POST /api/v1/ai/recommendations/generate (기존 유지)
# ---------------------------------------------------------------------------

class RecommendationRequest(_CamelModel):
    """POST /api/v1/ai/recommendations/generate 요청 (기존 하위호환 유지)."""
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
    tech_stacks: list[str] = Field(default_factory=list)


class RecommendationResponse(_CamelModel):
    """POST /api/v1/ai/recommendations/generate 응답 (기존 하위호환 유지)."""
    user_id: int
    recommendation_reason: RecommendationReason
    next_nodes: list[RecommendationNextNode]
    references: list[RecommendationReference]


# ---------------------------------------------------------------------------
# 자정 배치 추천 — POST /api/v1/ai/recommendations/daily-generate
# ---------------------------------------------------------------------------

class DailyRecommendationRequest(_CamelModel):
    """POST /api/v1/ai/recommendations/daily-generate 요청.

    SpringBoot 자정 배치 스케줄러 또는 온보딩 직후 커리큘럼 confirm 후 호출.
    결과는 Redis recommendation:{userId}:{curriculumId} 에 저장됨 (TTL 24h).
    """
    user_id: int
    curriculum_id: int
    current_level: UserLevel
    favorite_tech_stacks: list[str] = Field(default_factory=list)
    skill_stats: list[SkillStat] = Field(default_factory=list)
    recent_activities: list[ActivityHistoryItem] = Field(default_factory=list)
    google_calendar_events: list[GoogleCalendarEvent] = Field(default_factory=list)
    recent_curricula_ids: list[int] = Field(default_factory=list)  # 중복 방지용
    current_node_title: str | None = None
    current_node_description: str | None = None
    current_node_date: str | None = None
    current_node_tech_stacks: list[str] = Field(default_factory=list)


class CurrentStatus(_CamelModel):
    """현재 학습 상태 요약."""
    summary: str         # 1문장
    detail: str          # 2~3문장
    top_skills: list[str]


class DailyRecommendationPayload(_CamelModel):
    """POST /api/v1/ai/recommendations/daily-generate 응답.

    SpringBoot가 Redis recommendation:{userId}:{curriculumId} 에 저장.
    사용자가 추천 페이지에서 카드 상세조회 시 이 payload를 FE로 반환.
    """
    user_id: int
    curriculum_id: int
    recommendation_reason: RecommendationReason
    current_status: CurrentStatus
    references: list[RecommendationReference]
    next_nodes: list[RecommendationNextNode]


# ---------------------------------------------------------------------------
# 성장 일지 요약 — POST /api/v1/ai/growth/summary
# ---------------------------------------------------------------------------

class GrowthStats(_CamelModel):
    """SpringBoot GET /activities/growth-report 에서 집계한 통계 7종."""
    top_tech_stacks: list[str]
    total_activity_count: int
    recent_growth_tech: str | None = None
    max_streak_days: int
    tech_score_snapshot: dict[str, int] = Field(default_factory=dict)
    monthly_activity_counts: dict[str, dict[str, int]] = Field(default_factory=dict)
    tech_activity_ranking: list[str] = Field(default_factory=list)


class GrowthSummaryRequest(_CamelModel):
    """POST /api/v1/ai/growth/summary 요청."""
    user_id: int
    stats: GrowthStats


class GrowthSummaryResponse(_CamelModel):
    """POST /api/v1/ai/growth/summary 응답."""
    summary: str  # LLM이 생성한 한 문장 성장 요약


# ---------------------------------------------------------------------------
# 프로필 피드백 재분석 — POST /api/v1/ai/profile/feedback
# ---------------------------------------------------------------------------

class PreviousAnalysis(_CamelModel):
    """브라우저 Cache에 저장된 이전 분석 결과."""
    summary: str
    tech_details: list[dict[str, Any]] = Field(default_factory=list)
    recommended_positions: list[dict[str, Any]] = Field(default_factory=list)


class ProfileFeedbackRequest(_CamelModel):
    """POST /api/v1/ai/profile/feedback 요청."""
    user_id: int
    previous_analysis: PreviousAnalysis
    user_feedback: str


class ProfileFeedbackResponse(_CamelModel):
    """POST /api/v1/ai/profile/feedback 응답."""
    summary: str
    tech_details: list[dict[str, Any]] = Field(default_factory=list)
    recommended_positions: list[dict[str, Any]] = Field(default_factory=list)


# ---------------------------------------------------------------------------
# 폴백 / 에러 봉투
# ---------------------------------------------------------------------------

class MinimalFallbackResponse(BaseModel):
    """LLM 구조화 실패 시 최소 보장 응답."""
    error: str
    fallback: bool = True
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta | None = None

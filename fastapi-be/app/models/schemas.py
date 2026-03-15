"""
Pydantic schemas for Spring Boot ↔ FastAPI internal API contracts.

Design principles (from AI Tech Spec v3):
- FastAPI receives a fully assembled AIContext from Spring Boot.
- FastAPI never queries MySQL directly.
- All responses include model_meta, references, and assumptions/warnings.
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any

from pydantic import BaseModel, Field, HttpUrl


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


class QuizType(str, Enum):
    pre_assessment = "pre_assessment"   # 사전 평가
    review = "review"                   # 복습
    interview = "interview"             # 면접 대비


class QuestionType(str, Enum):
    short_answer = "short_answer"
    multiple_choice = "multiple_choice"
    coding = "coding"


class RecommendationRequestType(str, Enum):
    reference = "reference"     # 레퍼런스 추천
    activity = "activity"       # 활동 추천 (퀴즈/커리큘럼 유도)


class FreshnessGrade(str, Enum):
    stable = "stable"           # 최신, 신뢰 가능
    warning = "warning"         # 오래됐을 수 있음, 확인 필요
    outdated = "outdated"       # 내용이 구식일 가능성 높음


class NodeType(str, Enum):
    study = "study"             # 이론/레퍼런스 학습
    action = "action"           # 실습/코드 작성
    review = "review"           # 복습/점검


# ---------------------------------------------------------------------------
# Shared sub-models
# ---------------------------------------------------------------------------

class ModelMeta(BaseModel):
    """LLM 호출 메타데이터 — Spring Boot가 저장하는 추적 정보."""
    model: str = Field(..., examples=["gpt-4o-mini", "gpt-4o"])
    prompt_version: str = Field(..., examples=["extract_v2", "recommend_v3"])
    policy_version: str = Field(default="policy_v1")


class ReferenceItem(BaseModel):
    """Qdrant에서 검색된 레퍼런스 문서 단위."""
    doc_id: str = Field(..., examples=["ref_jwt_001"])
    title: str
    url: str = Field(..., examples=["https://example.com/jwt-bcp"])
    freshness: FreshnessGrade = FreshnessGrade.stable
    skill_tags: list[str] = Field(default_factory=list, examples=[["JWT", "Spring Security"]])
    source_type: str = Field(default="unknown", examples=["official_doc", "tech_blog", "curated"])


# ---------------------------------------------------------------------------
# AIContext — input from Spring Boot
# ---------------------------------------------------------------------------

class SurveyContext(BaseModel):
    """온보딩 설문 결과."""
    job_role: str = Field(..., examples=["취준생", "현직 개발자"])
    target_positions: list[str] = Field(..., examples=[["backend", "ai"]])
    tech_stacks: list[str] = Field(..., examples=[["Spring", "FastAPI", "Vue"]])
    persona: str = Field(default="default")


class UserContext(BaseModel):
    """Spring Boot가 조합해서 넘기는 사용자 기본 정보."""
    user_id: int
    job_role: str = Field(..., examples=["취준생"])
    target_positions: list[str] = Field(default_factory=list, examples=[["backend", "ai"]])
    survey_context: SurveyContext | None = None


class LearningState(BaseModel):
    """
    학습 상태 — Spring Boot가 스코어링 알고리즘으로 계산한 결과.
    FastAPI는 이 값을 받아서 추천 우선순위 판단에만 사용한다.
    """
    skill: str = Field(..., examples=["Spring Security"])
    raw_score: float = Field(..., ge=0.0, description="누적 가중 점수")
    relative_rank: RelativeRank = Field(
        ..., description="사용자 내 상대 순위 (3안: 개인 내 비교)"
    )
    percentile: int = Field(
        ..., ge=0, le=100, description="동일 기술 전체 사용자 백분위 (2안: 시장 비교)"
    )


class RecentActivity(BaseModel):
    """Spring Boot가 요약해서 넘기는 최근 활동 정보."""
    source: Source
    category: Category
    tech_stacks: list[str]
    summary: str
    activity_date: datetime | None = None


class CalendarConstraint(BaseModel):
    """일정 제약 — 커리큘럼 스케줄링에 사용."""
    date: str = Field(..., examples=["2026-03-10"], description="YYYY-MM-DD")
    busy_slots: list[str] = Field(
        default_factory=list, examples=[["19:00-21:00"]]
    )


class SourceItem(BaseModel):
    """
    /internal/analyze/activities 요청 단위.
    Spring Boot가 GitHub/Velog/Calendar에서 가져온 원시 텍스트 1건.
    """
    source: Source
    source_item_id: str = Field(
        ..., examples=["velog:post:12345", "github:repo:team/kairos:README.md"]
    )
    title: str
    text: str
    metadata: dict[str, Any] = Field(default_factory=dict)


# ---------------------------------------------------------------------------
# Request bodies — sent BY Spring Boot TO FastAPI
# ---------------------------------------------------------------------------

class AnalyzeActivitiesRequest(BaseModel):
    """POST /internal/analyze/activities"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    source_items: list[SourceItem] = Field(..., min_length=1)


class RecommendationRequest(BaseModel):
    """POST /internal/recommendations"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    learning_states: list[LearningState]
    recent_activities: list[RecentActivity]
    history_exclusions: list[str] = Field(
        default_factory=list,
        description="이미 추천된 doc_id 또는 태그 — 중복 방지용",
    )
    calendar_constraints: list[CalendarConstraint] = Field(default_factory=list)
    request_type: RecommendationRequestType = RecommendationRequestType.reference


class QuizRequest(BaseModel):
    """POST /internal/quizzes"""
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


class CurriculumRequest(BaseModel):
    """POST /internal/curriculums"""
    request_id: str
    trace_id: str
    policy_version: str = "policy_v1"
    user: UserContext
    goals: list[str] = Field(..., min_length=1, examples=[["JWT 심화", "테스트 코드"]])
    learning_states: list[LearningState]
    calendar_constraints: list[CalendarConstraint]
    accepted_references: list[ReferenceItem] = Field(default_factory=list)
    duration_days: int = Field(default=5, ge=1, le=30)


# ---------------------------------------------------------------------------
# Response schemas — returned BY FastAPI TO Spring Boot
# ---------------------------------------------------------------------------

class ActivityResult(BaseModel):
    """활동 정규화 결과 1건 — user_activity 테이블 저장용."""
    source: Source
    source_item_id: str
    category: Category
    tech_stacks: list[str]
    summary: str
    activity_date: datetime | None = None
    confidence: float = Field(..., ge=0.0, le=1.0)


class AnalysisSummary(BaseModel):
    """전체 분석 요약 — analysis_results 저장용."""
    observed_strengths: list[str]
    observed_gaps: list[str]
    recommended_positions: list[str]


class AnalyzeActivitiesResponse(BaseModel):
    """POST /internal/analyze/activities 응답."""
    activities: list[ActivityResult]
    analysis_summary: AnalysisSummary
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta


class RecommendationItem(BaseModel):
    """추천 항목 1건."""
    type: RecommendationRequestType
    title: str
    reason: str = Field(..., description="이 항목을 추천하는 구체적 근거")
    difficulty: RelativeRank
    estimated_time: str = Field(..., examples=["25분"])
    doc_id: str | None = None


class RecommendationResponse(BaseModel):
    """POST /internal/recommendations 응답."""
    items: list[RecommendationItem]
    reason: str = Field(..., description="이번 추천 전체를 관통하는 전략적 이유")
    assumptions: list[str] = Field(
        default_factory=list,
        description="추천 생성 시 가정한 조건 (일정, 학습 강도 등)",
    )
    references: list[ReferenceItem]
    outdated_warning: str | None = Field(
        default=None, description="검색 결과 중 오래된 문서가 있을 경우 경고 메시지"
    )
    model_meta: ModelMeta


class QuizQuestion(BaseModel):
    """퀴즈 문항 1건."""
    id: int
    type: QuestionType
    question: str
    expected_points: list[str] = Field(
        default_factory=list, description="채점 기준이 되는 핵심 포인트"
    )
    options: list[str] | None = Field(
        default=None, description="객관식인 경우에만 사용"
    )


class QuizRubric(BaseModel):
    """채점 기준."""
    full_score_criteria: list[str]
    partial_score_criteria: list[str] = Field(default_factory=list)


class QuizResponse(BaseModel):
    """POST /internal/quizzes 응답."""
    quiz_type: QuizType
    questions: list[QuizQuestion]
    rubric: QuizRubric
    references: list[ReferenceItem] = Field(default_factory=list)
    model_meta: ModelMeta


class CurriculumNode(BaseModel):
    """커리큘럼 단계 1건."""
    day: int = Field(..., ge=1)
    title: str
    type: NodeType
    estimated_time: str = Field(..., examples=["40분"])
    description: str | None = None
    reference_doc_ids: list[str] = Field(default_factory=list)


class CurriculumResponse(BaseModel):
    """POST /internal/curriculums 응답."""
    duration_days: int
    why: str = Field(..., description="이 커리큘럼이 현재 사용자에게 적합한 이유")
    assumptions: list[str] = Field(default_factory=list)
    nodes: list[CurriculumNode]
    references: list[ReferenceItem] = Field(default_factory=list)
    model_meta: ModelMeta


# ---------------------------------------------------------------------------
# Fallback / error envelope
# ---------------------------------------------------------------------------

class MinimalFallbackResponse(BaseModel):
    """
    LLM 구조화 실패 시 FastAPI가 반환하는 최소 보장 응답.
    Spring Boot는 이 필드를 항상 파싱할 수 있어야 한다.
    """
    error: str
    fallback: bool = True
    references: list[ReferenceItem] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    model_meta: ModelMeta | None = None

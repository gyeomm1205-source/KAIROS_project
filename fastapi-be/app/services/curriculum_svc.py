"""
커리큘럼 생성 서비스 — POST /api/v1/ai/curriculum/generate

실행 흐름 (3분기):
    run_curriculum(request)
        │
        ├─ ONBOARDING: _build_onboarding_context()  ← analysisData + userTechStacks + 캘린더
        ├─ AUTO:       _build_auto_context()         ← recentActivities + userTechStacks + 캘린더
        └─ MANUAL:     _build_manual_context()       ← 사용자 직접 지정 + 캘린더
        │
        ├─ _generate_with_llm(ctx, curriculum_type)  ← gpt-4o structured output
        └─ _assemble_response(output)                ← CurriculumResponse 조립

소유 규칙:
  - 이 서비스는 MySQL에 직접 쿼리하지 않는다.
  - 모든 도메인 컨텍스트는 CurriculumRequest를 통해 전달된다(Spring Boot 조립).
"""

from __future__ import annotations

import logging
from datetime import date, timedelta
from typing import Any

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name
from app.models.schemas import (
    ActivityHistoryItem,
    AnalysisData,
    CurriculumNode,
    CurriculumRecommendationReason,
    CurriculumRequest,
    CurriculumResponse,
    CurriculumType,
    GoogleCalendarEvent,
    ManualGeneration,
    SkillStat,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# LLM 구조화 출력 스키마
# ---------------------------------------------------------------------------

class _CurriculumNodeOutput(BaseModel):
    title: str = Field(..., description="학습 주제 제목")
    description: str | None = Field(None, description="title을 구체적으로 풀어쓴 설명. 무엇을 어떻게 학습하는지 1~2문장.")
    scheduled_date: str = Field(..., description="학습 예정일 (YYYY-MM-DD)")
    expected_minutes: int = Field(..., ge=30, le=240, description="예상 소요 시간(분)")


class _CurriculumOutput(BaseModel):
    """LLM 구조화 출력 스키마."""
    summary_line: str = Field(
        description="커리큘럼을 한 줄로 요약한 헤드라인. 예: '실무 도입을 앞둔 GraphQL 단기 마스터 과정'"
    )
    user_context: str = Field(
        description="사용자의 현재 상황 요약. 기존 경험, 수준, 목표 포지션 등 1~2문장."
    )
    ai_interpretation: str = Field(
        description="AI가 이해한 학습 방향. 어떤 내용을 왜 이 순서로 편성했는지 1~2문장."
    )
    curriculum_rationale: str = Field(
        description="일정 배치 근거. 캘린더 이벤트 회피, 학습량 분산 등 1~2문장."
    )
    nodes: list[_CurriculumNodeOutput] = Field(
        description="날짜별 학습 노드 목록. 시작일부터 순서대로."
    )


# ---------------------------------------------------------------------------
# 공통 원칙 프롬프트
# ---------------------------------------------------------------------------

_COMMON_RULES = """\
[공통 원칙]
1. 오늘 날짜({today})부터 시작해 노드를 배치하세요.
2. 커리큘럼 기간은 아래 기준에 따라 결정하세요. 7일을 절대 초과하지 마세요.
   - 단일 개념 학습 (Git 기초, 특정 라이브러리 입문 등): 2~3일
   - 개념 이해 + 실습이 필요한 주제: 4~5일
   - 여러 기술 통합 또는 심화가 필요한 주제: 6~7일
3. 바쁜 날짜({busy_dates})는 건너뛰고 나머지 날에만 노드를 할당하세요.
4. 하루 1개 노드 원칙: 같은 scheduled_date를 가진 노드는 없어야 합니다.
5. 노드 순서는 개념 이해 → 실습 → 심화/복습 흐름으로 구성하세요.
6. expected_minutes는 30~120분 범위에서 실제 학습량에 맞게 설정하세요.
7. 노드 제목과 내용은 실제 학습 활동만 포함하세요. "Q&A 세션", "전문가 상담" 같이 혼자 할 수 없는 활동은 절대 포함하지 마세요.
8. 반드시 한국어로 작성하세요.\
"""

# ── ONBOARDING 프롬프트 ──────────────────────────────────────────────────────
_ONBOARDING_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
신규 가입자의 GitHub/Velog 활동 분석 결과와 기술 숙련도 데이터를 바탕으로,
이 사람에게 지금 가장 필요한 학습 주제를 직접 결정하고 날짜가 지정된 커리큘럼을 설계합니다.

[주제 선정 기준 — 반드시 아래 순서로 판단하세요]
1. 사용 빈도 상위 기술 중 심화 학습 시 임팩트가 큰 것을 우선합니다.
2. 추천 포지션(recommendedPositions)과 연관성이 높은 기술을 선호합니다.
3. 단순 반복(알고리즘 풀이, README 수정 등)보다 실제 프로젝트 기술에 집중합니다.
4. 주제는 하나의 구체적인 기술 또는 기술 조합으로 한정하세요.

""" + _COMMON_RULES

_ONBOARDING_USER_TEMPLATE = """\
[프로필 분석 결과]
- 분석 요약: {summary}
- 주요 기술 스택 (사용 빈도순):
{tech_details}

[기술 숙련도 통계 (DB 기반)]
{tech_stats}

[일정 정보]
- 오늘: {today}
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 분석 결과를 바탕으로 이 사람에게 가장 적합한 학습 주제를 직접 결정하고 커리큘럼을 생성하세요. 기간은 주제 복잡도에 맞게 자유롭게 결정하세요 (최대 7일).

[노드 작성 기준]
- title: 무엇을 배우는지 명확히 드러나도록 구체적으로 작성하세요.
- description: 위 분석 결과를 통해 파악된 사용자의 현재 수준에 딱 맞춰서, 이 학습 노드에서 수행해야 할 구체적인 실천 목표와 가이드를 2~3문장으로 제시하세요. 템플릿처럼 딱딱하게 쓰지 말고, 멘토가 조언하듯 자연스럽게 작성하세요.\
"""

# ── AUTO 프롬프트 ────────────────────────────────────────────────────────────
_AUTO_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자의 최근 활동 이력과 기술 숙련도 통계를 바탕으로,
지금 이 시점에 가장 적합한 학습 주제를 스스로 결정하고 날짜가 지정된 커리큘럼을 설계합니다.

[주제 선정 기준]
1. 최근 활동에서 자주 등장하는 기술 중 심화할 여지가 있는 것을 우선합니다.
2. 자주 쓰지만 숙련도가 상대적으로 낮은 기술을 보완하는 방향을 우선합니다.
3. 단순 반복 작업보다 새로운 개념 습득이나 실전 적용에 집중합니다.

""" + _COMMON_RULES

_AUTO_USER_TEMPLATE = """\
[최근 활동 이력 (최신 순)]
{recent_activities}

[기술 숙련도 통계 (DB 기반, 활동 빈도 순)]
{tech_stats}

[일정 정보]
- 오늘: {today}
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 활동 이력과 기술 통계를 바탕으로 이 사람에게 지금 가장 적합한 학습 주제를 직접 결정하고 커리큘럼을 생성하세요. 기간은 주제 복잡도에 맞게 자유롭게 결정하세요 (최대 7일).

[노드 작성 기준]
- title: 무엇을 배우는지 명확히 드러나도록 구체적으로 작성하세요.
- description: 위 분석 결과를 통해 파악된 사용자의 현재 수준에 딱 맞춰서, 이 학습 노드에서 수행해야 할 구체적인 실천 목표와 가이드를 2~3문장으로 제시하세요. 템플릿처럼 딱딱하게 쓰지 말고, 멘토가 조언하듯 자연스럽게 작성하세요.\
"""

# ── MANUAL 프롬프트 ──────────────────────────────────────────────────────────
_MANUAL_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자가 직접 지정한 학습 주제와 목표를 바탕으로 날짜가 지정된 커리큘럼을 설계합니다.

""" + _COMMON_RULES

_MANUAL_USER_TEMPLATE = """\
[사용자 정보]
- 학습 주제: {topic}
- 학습 목표 유형: {goal_type}
- 구체적 목표: {specific_goal}
- 기술 숙련도 통계 (참고용):
{tech_stats}

[설문 응답]
{survey_text}

[일정 정보]
- 오늘: {today}
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 정보를 바탕으로 커리큘럼을 생성하세요. 기간은 주제 복잡도에 맞게 자유롭게 결정하세요 (최대 7일).

[노드 작성 기준]
- title: 무엇을 배우는지 명확히 드러나도록 구체적으로 작성하세요.
- description: title을 보고 해당 내용의 학습을 위해 수행할 구체적인 행동을 3단계 레벨로 나누어 제시하세요.
  완전히 처음 접하는 사람에게 추천할 학습: (구체적 행동)
  가볍게 다루거나 공부해본 수준의 사람에게 추천할 학습: (구체적 행동)
  실제로 해당 기술을 써서 프로젝트를 진행해본 경험이 있는 사람에게 추천할 실습: (구체적 행동)\
"""


# ---------------------------------------------------------------------------
# 공개 엔트리 포인트
# ---------------------------------------------------------------------------

async def run_curriculum(request: CurriculumRequest) -> CurriculumResponse:
    """POST /api/v1/ai/curriculum/generate 의 메인 엔트리 포인트."""
    logger.info(
        "[run_curriculum] user_id=%s type=%s",
        request.user_id,
        request.curriculum_type.value,
    )

    try:
        output = await _generate_with_llm(request)
    except Exception as exc:
        logger.warning("[run_curriculum] LLM 호출 실패 (%s) — 템플릿 폴백 사용", exc)
        output = _generate_with_template(request)

    response = _assemble_response(output)
    logger.info("[run_curriculum] 완료 — %d개 노드 생성 (type=%s)", len(response.nodes), request.curriculum_type.value)
    return response


# ---------------------------------------------------------------------------
# 내부 구현
# ---------------------------------------------------------------------------

async def _generate_with_llm(request: CurriculumRequest) -> _CurriculumOutput:
    """3분기별 프롬프트로 gpt-4o structured output 커리큘럼 생성."""
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.7)
    structured_llm = llm.with_structured_output(_CurriculumOutput)

    match request.curriculum_type:
        case CurriculumType.onboarding:
            system_prompt = _ONBOARDING_SYSTEM_PROMPT
            user_template = _ONBOARDING_USER_TEMPLATE
            ctx = _build_onboarding_context(request)
        case CurriculumType.auto:
            system_prompt = _AUTO_SYSTEM_PROMPT
            user_template = _AUTO_USER_TEMPLATE
            ctx = _build_auto_context(request)
        case CurriculumType.manual:
            system_prompt = _MANUAL_SYSTEM_PROMPT
            user_template = _MANUAL_USER_TEMPLATE
            ctx = _build_manual_context(request)
        case _:
            logger.warning("[_generate_with_llm] 알 수 없는 curriculum_type=%s — ONBOARDING으로 폴백", request.curriculum_type)
            system_prompt = _ONBOARDING_SYSTEM_PROMPT
            user_template = _ONBOARDING_USER_TEMPLATE
            ctx = _build_onboarding_context(request)

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human",  user_template),
    ])
    chain = prompt | structured_llm
    result: _CurriculumOutput = await chain.ainvoke(ctx)

    logger.debug(
        "[_generate_with_llm] model=%s type=%s nodes=%d",
        get_model_name(TaskType.RECOMMENDATION_REASON),
        request.curriculum_type.value,
        len(result.nodes),
    )
    return result


# ---------------------------------------------------------------------------
# 분기별 context 빌더
# ---------------------------------------------------------------------------

def _build_onboarding_context(request: CurriculumRequest) -> dict[str, Any]:
    """ONBOARDING: profile_analyzer 결과 + user_tech_stacks + 캘린더."""
    today = date.today().isoformat()
    busy_dates = _fmt_busy_dates(request.google_calendar_events, request.consider_personal_schedule)
    analysis = request.analysis_data

    # tech_details: 사용 빈도 내림차순 정렬, 상위 7개
    tech_details_lines: list[str] = []
    if analysis and analysis.tech_details:
        sorted_tech = sorted(analysis.tech_details, key=lambda t: t.usage_count, reverse=True)
        for t in sorted_tech[:7]:
            tech_details_lines.append(
                f"  - {t.tech_name}: 사용 {t.usage_count}회, 숙련도 {t.proficiency_percentage}%"
            )
    tech_details_str = "\n".join(tech_details_lines) if tech_details_lines else "  (정보 없음)"

    # recommended_positions
    positions_str = "없음"
    if analysis and analysis.recommended_positions:
        pos_parts = [f"{p.position_name}({p.fit_level})" for p in analysis.recommended_positions]
        positions_str = ", ".join(pos_parts)

    # user_tech_stacks (DB 집계)
    tech_stats_str = _fmt_skill_stats(request.user_tech_stacks)

    return {
        "summary":               (analysis.summary if analysis else "정보 없음"),
        "tech_details":          tech_details_str,
        "recommended_positions": positions_str,
        "tech_stats":            tech_stats_str,
        "today":                 today,
        "busy_dates":            busy_dates,
    }


def _build_auto_context(request: CurriculumRequest) -> dict[str, Any]:
    """AUTO: recent_activities + user_tech_stacks + 캘린더."""
    today = date.today().isoformat()
    busy_dates = _fmt_busy_dates(request.google_calendar_events, request.consider_personal_schedule)

    # recent_activities (최신 10개)
    activities_lines: list[str] = []
    for act in request.recent_activities[:10]:
        stacks = ", ".join(act.tech_stacks) or "없음"
        activities_lines.append(
            f"  - [{act.activity_date[:10]}] [{act.activity_type}] {act.title} (기술: {stacks}, 분류: {act.category})"
        )
    activities_str = "\n".join(activities_lines) if activities_lines else "  (최근 활동 없음)"

    return {
        "recent_activities": activities_str,
        "tech_stats":        _fmt_skill_stats(request.user_tech_stacks),
        "today":             today,
        "busy_dates":        busy_dates,
    }


def _build_manual_context(request: CurriculumRequest) -> dict[str, Any]:
    """MANUAL: 사용자 직접 지정 주제 + user_tech_stacks + 캘린더."""
    today = date.today().isoformat()
    busy_dates = _fmt_busy_dates(request.google_calendar_events, request.consider_personal_schedule)
    manual = request.manual_generation

    return {
        "topic":        (manual.topic or "개발 역량 강화") if manual else "개발 역량 강화",
        "goal_type":    (manual.goal_type or "전반적 학습") if manual else "전반적 학습",
        "specific_goal": (manual.specific_goal or "실무 수준 달성") if manual else "실무 수준 달성",
        "tech_stats":   _fmt_skill_stats(request.user_tech_stacks),
        "survey_text":  _fmt_survey(manual) if manual else "없음",
        "today":        today,
        "busy_dates":   busy_dates,
    }


# ---------------------------------------------------------------------------
# 폴백 — LLM 실패 시 규칙 기반 최소 커리큘럼
# ---------------------------------------------------------------------------

def _generate_with_template(request: CurriculumRequest) -> _CurriculumOutput:
    """규칙 기반 폴백 — LLM 호출 없음."""
    # 주제 결정
    match request.curriculum_type:
        case CurriculumType.onboarding:
            if request.analysis_data and request.analysis_data.tech_details:
                top = max(request.analysis_data.tech_details, key=lambda t: t.usage_count)
                topic = f"{top.tech_name} 심화 학습"
            elif request.user_tech_stacks:
                topic = f"{request.user_tech_stacks[0].skill} 심화 학습"
            else:
                topic = "개발 역량 강화"
        case CurriculumType.auto:
            if request.user_tech_stacks:
                topic = f"{request.user_tech_stacks[0].skill} 심화 학습"
            else:
                topic = "개발 역량 강화"
        case CurriculumType.manual:
            manual = request.manual_generation
            topic = (manual.topic or "학습 목표") if manual else "학습 목표"
        case _:
            topic = "개발 역량 강화"

    today = date.today()
    busy = _get_busy_date_set(request.google_calendar_events, request.consider_personal_schedule)

    nodes: list[_CurriculumNodeOutput] = []
    current = today
    step_labels = ["개념 이해", "실습", "심화 및 복습"]
    step_minutes = [60, 90, 60]

    for _ in range(14):  # 최대 14일 탐색
        if len(nodes) >= 5:
            break
        if current.isoformat() not in busy:
            idx = len(nodes)
            label = step_labels[idx % 3]
            mins = step_minutes[idx % 3]
            nodes.append(_CurriculumNodeOutput(
                title=f"{topic} — {label}",
                description=None,
                scheduled_date=current.isoformat(),
                expected_minutes=mins,
            ))
        current += timedelta(days=1)

    return _CurriculumOutput(
        summary_line=f"{topic} 단기 집중 과정",
        user_context="프로필 기반으로 맞춤 커리큘럼을 구성했습니다.",
        ai_interpretation=f"{topic} 학습을 개념 → 실습 → 심화 순서로 편성했습니다.",
        curriculum_rationale="일정 제약을 반영해 학습 가능한 날짜에만 노드를 배치했습니다.",
        nodes=nodes,
    )


def _assemble_response(output: _CurriculumOutput) -> CurriculumResponse:
    """LLM 출력을 CurriculumResponse로 조립한다."""
    reason = CurriculumRecommendationReason(
        summary_line=output.summary_line,
        user_context=output.user_context,
        ai_interpretation=output.ai_interpretation,
        curriculum_rationale=output.curriculum_rationale,
    )
    nodes = [
        CurriculumNode(
            title=n.title,
            description=n.description,
            scheduled_date=n.scheduled_date,
            expected_minutes=n.expected_minutes,
        )
        for n in output.nodes
    ]
    return CurriculumResponse(recommendation_reason=reason, nodes=nodes)


# ---------------------------------------------------------------------------
# 헬퍼
# ---------------------------------------------------------------------------

def _fmt_skill_stats(skill_stats: list[SkillStat], top_n: int = 8) -> str:
    """SkillStat 목록을 프롬프트용 문자열로 변환 (빈도 내림차순)."""
    if not skill_stats:
        return "  (기술 통계 없음)"
    sorted_stats = sorted(skill_stats, key=lambda s: s.count, reverse=True)
    lines = [f"  - {s.skill}: {s.count}회" for s in sorted_stats[:top_n]]
    return "\n".join(lines)


def _fmt_survey(manual: ManualGeneration) -> str:
    if not manual.survey_answers:
        return "없음"
    labels = {
        "q1Concept":    "현재 이해 수준",
        "q2Experience": "관련 경험",
        "q3Reason":     "학습 이유",
    }
    lines = [f"- {labels.get(k, k)}: {v}" for k, v in manual.survey_answers.items()]
    return "\n".join(lines) or "없음"


def _fmt_busy_dates(events: list[GoogleCalendarEvent], consider: bool) -> str:
    if not consider or not events:
        return "없음"
    return ", ".join(f"{e.title} ({e.start_date} ~ {e.end_date})" for e in events)


def _get_busy_date_set(events: list[GoogleCalendarEvent], consider: bool) -> set[str]:
    """바쁜 날짜를 YYYY-MM-DD 집합으로 반환한다."""
    if not consider or not events:
        return set()
    busy: set[str] = set()
    for e in events:
        try:
            start = date.fromisoformat(e.start_date)
            end = date.fromisoformat(e.end_date)
            cur = start
            while cur <= end:
                busy.add(cur.isoformat())
                cur += timedelta(days=1)
        except ValueError:
            continue
    return busy

"""
커리큘럼 생성 서비스 — POST /api/v1/ai/curriculum/generate
이슈: S14P21A506-147

실행 흐름:
    run_curriculum(request)
        │
        ├─ 1. _build_context()         ← profileData + manualGeneration → 프롬프트 변수 조립
        ├─ 2. _generate_with_llm()     ← gpt-4o LLM 호출 (structured output)
        └─ 3. _assemble_response()     ← CurriculumResponse 조립

소유 규칙:
  - 이 서비스는 MySQL에 직접 쿼리하지 않는다.
  - 모든 도메인 컨텍스트는 CurriculumRequest를 통해 전달된다.
"""

from __future__ import annotations

import logging
from datetime import date, timedelta
from typing import Any

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name, get_prompt_version
from app.models.schemas import (
    CurriculumNode,
    CurriculumRecommendationReason,
    CurriculumRequest,
    CurriculumResponse,
    GoogleCalendarEvent,
    ManualGeneration,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# LLM 구조화 출력 스키마
# ---------------------------------------------------------------------------

class _CurriculumNodeOutput(BaseModel):
    title: str = Field(..., description="학습 주제 제목")
    description: str | None = Field(None, description="학습 내용 한 줄 설명")
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
# 프롬프트
# ---------------------------------------------------------------------------

_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자의 프로필, 학습 목표, 일정 제약을 바탕으로 날짜가 지정된 실현 가능한 학습 커리큘럼을 설계합니다.

[응답 원칙]
1. 오늘 날짜({today})부터 시작해 {total_days}일 범위 내에서 노드를 배치하세요.
2. 바쁜 날짜({busy_dates})는 건너뛰고 나머지 날에만 노드를 할당하세요.
3. 하루 1개 노드 원칙: 같은 scheduled_date를 가진 노드는 없어야 합니다.
4. 학습 목표({topic})를 달성하는 데 집중하세요.
5. 노드 순서는 개념 이해 → 실습 → 심화/복습 흐름으로 구성하세요.
6. expected_minutes는 30~120분 범위에서 실제 학습량에 맞게 설정하세요.
7. 반드시 한국어로 작성하세요.\
"""

_USER_TEMPLATE = """\
[사용자 정보]
- 학습 주제: {topic}
- 학습 목표 유형: {goal_type}
- 구체적 목표: {specific_goal}
- 수준: {user_level}
- 현재 기술 스택: {tech_stacks}
- 관심 포지션: {positions}

[설문 응답]
{survey_text}

[일정 정보]
- 오늘: {today}
- 권장 학습 기간: {total_days}일
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 정보를 바탕으로 커리큘럼을 생성하세요.\
"""


# ---------------------------------------------------------------------------
# 공개 엔트리 포인트
# ---------------------------------------------------------------------------

async def run_curriculum(request: CurriculumRequest) -> CurriculumResponse:
    """POST /api/v1/ai/curriculum/generate 의 메인 엔트리 포인트."""
    logger.info(
        "[run_curriculum] user_id=%s manual=%s",
        request.user_id,
        request.manual_generation.is_manual if request.manual_generation else False,
    )

    try:
        output = await _generate_with_llm(request)
    except Exception as exc:
        logger.warning("[run_curriculum] LLM 호출 실패 (%s) — 템플릿 폴백 사용", exc)
        output = _generate_with_template(request)

    response = _assemble_response(output)

    logger.info("[run_curriculum] 완료 — %d개 노드 생성", len(response.nodes))
    return response


# ---------------------------------------------------------------------------
# 내부 구현
# ---------------------------------------------------------------------------

async def _generate_with_llm(request: CurriculumRequest) -> _CurriculumOutput:
    """gpt-4o structured output으로 커리큘럼을 생성한다."""
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.4)
    structured_llm = llm.with_structured_output(_CurriculumOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM_PROMPT),
        ("human",  _USER_TEMPLATE),
    ])
    chain = prompt | structured_llm

    ctx = _build_context(request)
    result: _CurriculumOutput = await chain.ainvoke(ctx)

    logger.debug(
        "[_generate_with_llm] model=%s nodes=%d",
        get_model_name(TaskType.RECOMMENDATION_REASON),
        len(result.nodes),
    )
    return result


def _build_context(request: CurriculumRequest) -> dict[str, Any]:
    """프롬프트 주입용 컨텍스트를 조립한다."""
    manual = request.manual_generation
    profile = request.profile_data or {}

    topic        = (manual.topic or "개발 역량 강화") if manual else "개발 역량 강화"
    goal_type    = (manual.goal_type or "전반적 학습") if manual else "전반적 학습"
    specific_goal = (manual.specific_goal or "실무 수준 달성") if manual else "실무 수준 달성"
    survey_text  = _fmt_survey(manual) if manual else "없음"

    user_level   = _extract_level(profile)
    tech_stacks  = _extract_tech_stacks(profile)
    positions    = _extract_positions(profile)

    today        = date.today().isoformat()
    total_days   = _calc_total_days(manual)
    busy_dates   = _fmt_busy_dates(request.google_calendar_events, request.consider_personal_schedule)

    return {
        "topic":          topic,
        "goal_type":      goal_type,
        "specific_goal":  specific_goal,
        "user_level":     user_level,
        "tech_stacks":    tech_stacks,
        "positions":      positions,
        "survey_text":    survey_text,
        "today":          today,
        "total_days":     total_days,
        "busy_dates":     busy_dates,
    }


def _generate_with_template(request: CurriculumRequest) -> _CurriculumOutput:
    """규칙 기반 폴백 — LLM 호출 없음."""
    manual = request.manual_generation
    topic  = (manual.topic or "학습 목표") if manual else "학습 목표"
    today  = date.today()

    busy = _get_busy_date_set(request.google_calendar_events, request.consider_personal_schedule)
    total_days = _calc_total_days(manual)

    nodes: list[_CurriculumNodeOutput] = []
    current = today
    step    = 0
    for _ in range(total_days * 2):  # 충분한 날짜 탐색
        if len(nodes) >= total_days:
            break
        if current.isoformat() not in busy:
            idx = len(nodes)
            if idx == 0:
                node_title = f"{topic} 개념 이해"
                mins = 60
            elif idx % 3 == 1:
                node_title = f"{topic} 실습"
                mins = 90
            else:
                node_title = f"{topic} 심화 및 복습"
                mins = 60
            nodes.append(_CurriculumNodeOutput(
                title=node_title,
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
# 컨텍스트 추출 헬퍼
# ---------------------------------------------------------------------------

def _extract_level(profile: dict[str, Any]) -> str:
    return profile.get("level", profile.get("userLevel", "중급"))


def _extract_tech_stacks(profile: dict[str, Any]) -> str:
    stacks = profile.get("techStacks", profile.get("tech_stacks", []))
    if isinstance(stacks, list):
        return ", ".join(str(s) for s in stacks) or "미지정"
    return str(stacks) or "미지정"


def _extract_positions(profile: dict[str, Any]) -> str:
    positions = profile.get("desiredPositions", profile.get("positions", []))
    if isinstance(positions, list):
        return ", ".join(str(p) for p in positions) or "미지정"
    return str(positions) or "미지정"


def _calc_total_days(manual: ManualGeneration | None) -> int:
    """커리큘럼 기간(일)을 결정한다. 기본 14일."""
    if manual is None:
        return 14
    # 주제 복잡도에 따라 조정 가능 (현재는 기본값)
    return 14


def _fmt_survey(manual: ManualGeneration) -> str:
    if not manual.survey_answers:
        return "없음"
    lines = []
    labels = {
        "q1Concept":    "현재 이해 수준",
        "q2Experience": "관련 경험",
        "q3Reason":     "학습 이유",
    }
    for key, val in manual.survey_answers.items():
        label = labels.get(key, key)
        lines.append(f"- {label}: {val}")
    return "\n".join(lines) or "없음"


def _fmt_busy_dates(
    events: list[GoogleCalendarEvent],
    consider: bool,
) -> str:
    if not consider or not events:
        return "없음"
    parts = []
    for e in events:
        parts.append(f"{e.title} ({e.start_date} ~ {e.end_date})")
    return ", ".join(parts)


def _get_busy_date_set(
    events: list[GoogleCalendarEvent],
    consider: bool,
) -> set[str]:
    """바쁜 날짜를 YYYY-MM-DD 집합으로 반환한다."""
    if not consider or not events:
        return set()
    busy: set[str] = set()
    for e in events:
        try:
            start = date.fromisoformat(e.start_date)
            end   = date.fromisoformat(e.end_date)
            cur   = start
            while cur <= end:
                busy.add(cur.isoformat())
                cur += timedelta(days=1)
        except ValueError:
            continue
    return busy

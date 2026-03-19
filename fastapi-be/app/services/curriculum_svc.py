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
7. 노드 제목과 내용은 실제 학습 활동만 포함하세요. "Q&A 세션", "전문가 상담", "리뷰 세션" 같이 혼자 할 수 없는 활동은 절대 포함하지 마세요.
8. 반드시 한국어로 작성하세요.\
"""

# 수동 생성 (isManual=True): 유저가 직접 주제를 지정한 경우
_MANUAL_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자가 직접 지정한 학습 주제와 목표를 바탕으로 날짜가 지정된 커리큘럼을 설계합니다.

""" + _COMMON_RULES

_MANUAL_USER_TEMPLATE = """\
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
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 정보를 바탕으로 커리큘럼을 생성하세요. 기간은 주제 복잡도에 맞게 자유롭게 결정하세요 (최대 7일).\
"""

# 온보딩 자동 생성 (isManual=False): GitHub/Velog 분석 결과만으로 주제 자동 결정
_ONBOARDING_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
사용자의 GitHub/Velog 활동 분석 결과를 바탕으로 지금 이 사람에게 가장 적합한 학습 주제를 스스로 결정하고, 날짜가 지정된 커리큘럼을 설계합니다.

[주제 선정 기준 — 반드시 아래 순서로 판단하세요]
1. 사용 빈도 상위 기술 중 심화 학습 시 임팩트가 큰 것을 우선합니다.
2. 가능한 포지션(possible_positions)과 연관성이 높은 기술을 선호합니다.
3. 단순 반복(알고리즘 풀이, README 수정 등)보다 실제 프로젝트 기술에 집중합니다.
4. 주제는 하나의 구체적인 기술 또는 기술 조합으로 한정하세요. (예: "Spring Security", "React + TypeScript")

""" + _COMMON_RULES

_ONBOARDING_USER_TEMPLATE = """\
[GitHub/Velog 분석 결과]
- 한 줄 요약: {headline}
- 주요 기술 스택 (사용 빈도순 상위 5개): {top_skills}
- 경험 요약: {experience_summary}
- 학습 태도: {learning_attitude}
- 가능한 포지션: {possible_positions}
- 현재 수준: {user_level}

[일정 정보]
- 오늘: {today}
- 바쁜 기간(건너뛸 날짜): {busy_dates}

위 분석 결과를 바탕으로 이 사람에게 가장 적합한 학습 주제를 직접 결정하고 커리큘럼을 생성하세요. 기간은 주제 복잡도에 맞게 자유롭게 결정하세요 (최대 7일).\
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

    is_manual = request.manual_generation.is_manual if request.manual_generation else True
    if is_manual:
        system_prompt = _MANUAL_SYSTEM_PROMPT
        user_template = _MANUAL_USER_TEMPLATE
    else:
        system_prompt = _ONBOARDING_SYSTEM_PROMPT
        user_template = _ONBOARDING_USER_TEMPLATE

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human",  user_template),
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

    is_manual = manual.is_manual if manual else True
    today      = date.today().isoformat()
    busy_dates = _fmt_busy_dates(request.google_calendar_events, request.consider_personal_schedule)

    if is_manual:
        return {
            "topic":         (manual.topic or "개발 역량 강화") if manual else "개발 역량 강화",
            "goal_type":     (manual.goal_type or "전반적 학습") if manual else "전반적 학습",
            "specific_goal": (manual.specific_goal or "실무 수준 달성") if manual else "실무 수준 달성",
            "user_level":    _extract_level(profile),
            "tech_stacks":   _extract_tech_stacks(profile),
            "positions":     _extract_positions(profile),
            "survey_text":   _fmt_survey(manual) if manual else "없음",
            "today":         today,
            "busy_dates":    busy_dates,
        }
    else:
        return {
            "headline":           _extract_headline(profile),
            "top_skills":         _extract_top_skills(profile),
            "experience_summary": _extract_experience_summary(profile),
            "learning_attitude":  _extract_learning_attitude(profile),
            "possible_positions": _extract_possible_positions(profile),
            "user_level":         _extract_level(profile),
            "today":              today,
            "busy_dates":         busy_dates,
        }


def _generate_with_template(request: CurriculumRequest) -> _CurriculumOutput:
    """규칙 기반 폴백 — LLM 호출 없음."""
    manual = request.manual_generation
    topic  = (manual.topic or "학습 목표") if manual else "학습 목표"
    today  = date.today()

    busy = _get_busy_date_set(request.google_calendar_events, request.consider_personal_schedule)
    total_days = 7

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

def _extract_headline(profile: dict[str, Any]) -> str:
    return profile.get("headline", "분석 결과 없음")


def _extract_top_skills(profile: dict[str, Any], n: int = 5) -> str:
    freq = profile.get("skill_frequency", {})
    if not freq:
        return _extract_tech_stacks(profile)
    top = sorted(freq.items(), key=lambda x: x[1], reverse=True)[:n]
    return ", ".join(skill for skill, _ in top) or "미지정"


def _extract_experience_summary(profile: dict[str, Any]) -> str:
    return profile.get("experience_summary", "정보 없음")


def _extract_learning_attitude(profile: dict[str, Any]) -> str:
    return profile.get("learning_attitude", "정보 없음")


def _extract_possible_positions(profile: dict[str, Any]) -> str:
    positions = profile.get("possible_positions", [])
    if isinstance(positions, list):
        return ", ".join(str(p) for p in positions) or "미지정"
    return str(positions) or "미지정"


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

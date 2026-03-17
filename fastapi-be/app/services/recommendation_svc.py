"""
추천 서비스 — POST /internal/recommendations

실행 흐름:
    run_recommendation(request)
        │
        ├─ 1. retrieve_candidates()              ← TODO: Qdrant 연동으로 대체
        ├─ 2. _filter_history_exclusions()       ← 이미 본 doc_id 제거
        ├─ 3. _filter_by_skill_relevance()       ← 스킬 관련 자료만 유지
        ├─ 4. _rank_by_learning_priority()       ← 약점 스킬 우선 정렬 (spec §12-3)
        ├─ 5. _select_with_difficulty_balance()  ← 난이도 균형 선택
        ├─ 6. generate_recommendation_reason()   ← gpt-4o LLM 호출 (템플릿 폴백)
        └─ 7. _assemble_response()               ← RecommendationResponse 조립

소유 규칙 (AI Tech Spec v3):
  - 이 서비스는 MySQL에 직접 쿼리하지 않는다.
  - 모든 도메인 컨텍스트는 RecommendationRequest를 통해 전달된다 (Spring Boot 조립).
  - Qdrant가 이 서비스가 읽는 유일한 외부 저장소이다.
"""

from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.model_router import TaskType, get_model, get_model_name, get_prompt_version
from app.models.schemas import (
    FreshnessGrade,
    LearningState,
    ModelMeta,
    RecentActivity,
    RecommendationItem,
    RecommendationRequest,
    RecommendationRequestType,
    RecommendationResponse,
    ReferenceItem,
    RelativeRank,
    UserContext,
)

logger = logging.getLogger(__name__)

# 요청당 반환할 최대 자료 수
TOP_K = 5
MOCK_DATA_PATH = Path(__file__).parent.parent.parent / "tests" / "mock_recommendation_data.json"

# relative_rank → 추천 아이템 난이도 레이블 매핑
_RANK_TO_DIFFICULTY: dict[RelativeRank, RelativeRank] = {
    RelativeRank.low:  RelativeRank.low,
    RelativeRank.mid:  RelativeRank.mid,
    RelativeRank.high: RelativeRank.high,
}

# 랭킹 우선순위: 낮은 rank일수록 먼저 추천
_RANK_PRIORITY: dict[RelativeRank, int] = {
    RelativeRank.low:  0,  # 가장 우선 — 약점 먼저 보완
    RelativeRank.mid:  1,
    RelativeRank.high: 2,
}

# TOP_K 결과의 난이도 구성 비율: high : mid : low = 1 : 2 : 1
# Spring Boot에서 relative_rank는 30 / 40 / 30 percentile 기준으로 배정:
#   하위 30% → low, 중간 40% → mid, 상위 30% → high
_DIFFICULTY_RATIO: dict[RelativeRank, int] = {
    RelativeRank.low:  1,
    RelativeRank.mid:  2,
    RelativeRank.high: 1,
}


# 단계 1 — 후보 자료 조회

def retrieve_candidates(request: RecommendationRequest) -> list[dict[str, Any]]:
    """
    요청 컨텍스트에 맞는 후보 자료 목록을 반환한다.

    현재 구현: tests/mock_recommendation_data.json에서 목 데이터를 로드한다.
    운영 환경에서는 이 함수 본문을 Qdrant Self-Querying RAG 호출로 교체한다.

    TODO (Qdrant 연동):
        1. learning_states + recent_activities → query_text 생성
        2. target_positions + skill 태그 → 메타데이터 필터 생성
        3. qdrant_client.search() 호출 (limit=TOP_K * 3, over-fetch 후 필터)
        4. [hit.payload for hit in hits] 반환

    참고: AI Tech Design v3, §11 (Qdrant 설계), §5-2 (추천 파이프라인)
    """
    logger.info(
        "[retrieve_candidates] 목 모드 — %s 로드 중", MOCK_DATA_PATH
    )
    mock_data = _load_mock_data()
    return mock_data["mock_qdrant_hits"]


# 단계 2 — 이미 본 자료 제거

def _filter_history_exclusions(
    candidates: list[dict[str, Any]],
    exclusions: list[str],
) -> list[dict[str, Any]]:
    """history_exclusions에 포함된 doc_id를 가진 후보를 제거한다."""
    exclusion_set = set(exclusions)
    filtered = [c for c in candidates if c["doc_id"] not in exclusion_set]
    logger.debug(
        "[filter_exclusions] %d → %d개 (제거: %d개)",
        len(candidates), len(filtered), len(candidates) - len(filtered),
    )
    return filtered


# 단계 3 — 스킬 관련 자료만 유지

def _filter_by_skill_relevance(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
    recent_activities: list[RecentActivity],
) -> list[dict[str, Any]]:
    """
    사용자 스킬 범위와 겹치는 후보만 유지한다.

    스킬 범위 = learning_states의 스킬 + recent_activities의 tech_stacks.
    후보의 skill_tags 중 하나라도 스킬 범위와 부분 일치하면 관련 있는 자료로 판단
    (대소문자 무시, 부분 문자열 매칭 — 예: "Spring Security" ↔ "Spring").

    TODO (Qdrant 연동 시):
        실제 Qdrant에서는 이 필터를 payload filter로 내려보낼 수 있다.
        Python 측 필터는 safety net으로만 유지.
    """
    skill_universe: set[str] = set()
    for ls in learning_states:
        skill_universe.add(ls.skill.lower())
    for act in recent_activities:
        for tech in act.tech_stacks:
            skill_universe.add(tech.lower())

    def _is_relevant(candidate: dict[str, Any]) -> bool:
        for tag in candidate.get("skill_tags", []):
            if any(tag.lower() in su or su in tag.lower() for su in skill_universe):
                return True
        return False

    filtered = [c for c in candidates if _is_relevant(c)]
    logger.debug(
        "[filter_skill] %d → %d개 (스킬 범위 크기: %d)",
        len(candidates), len(filtered), len(skill_universe),
    )
    return filtered


# 단계 4 — 약점 스킬 우선 정렬

def _rank_by_learning_priority(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
) -> list[dict[str, Any]]:
    """
    약점 스킬을 다루는 자료가 앞에 오도록 정렬한다.

    정렬 기준 (spec §12-3 Hybrid 2+3):
      - 1순위: 가장 낮은 relative_rank 스킬 매칭 (low → mid → high)
      - 2순위: 가장 낮은 percentile (개선 여지가 큰 스킬 우선)
      - 3순위: Qdrant 유사도 점수 (목 데이터에 반영됨)

    매칭 스킬 태그가 없는 후보는 마지막에 배치된다.
    """
    rank_index: dict[str, tuple[int, int]] = {}
    for ls in learning_states:
        priority = _RANK_PRIORITY[ls.relative_rank]
        rank_index[ls.skill.lower()] = (priority, -ls.percentile)

    def _sort_key(candidate: dict[str, Any]) -> tuple[int, int, float]:
        best_priority = 99
        best_percentile_neg = 0
        for tag in candidate.get("skill_tags", []):
            for skill_lower, (prio, pct_neg) in rank_index.items():
                if tag.lower() in skill_lower or skill_lower in tag.lower():
                    if prio < best_priority:
                        best_priority = prio
                        best_percentile_neg = pct_neg
        qdrant_score = candidate.get("score", 0.0)
        return (best_priority, best_percentile_neg, -qdrant_score)

    return sorted(candidates, key=_sort_key)


# 단계 5 — 난이도 균형을 맞춰 top-K 선택

def _select_top_k(
    candidates: list[dict[str, Any]],
    k: int = TOP_K,
) -> list[dict[str, Any]]:
    """단순 top-K 슬라이스. 빈 결과 폴백 경로에서만 사용된다."""
    return candidates[:k]


def _select_with_difficulty_balance(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
    k: int = TOP_K,
) -> list[dict[str, Any]]:
    """
    1:2:1 난이도 구성(_DIFFICULTY_RATIO)을 유지하면서 k개를 선택한다.

    알고리즘:
      1. 각 후보를 난이도별 버킷으로 분류 (버킷 내 순서는 _rank_by_learning_priority 결과 유지).
      2. _DIFFICULTY_RATIO에 비례해 버킷별 할당량을 계산한다.
         반올림 잉여 슬롯은 low → mid 순으로 우선 배정한다.
      3. 각 버킷을 할당량까지 채우고, 부족한 버킷의 잉여분은 다음 우선순위 버킷에서 보충한다.

    모든 후보의 난이도가 동일한 경우(예: 전부 high) 단순 top-K 슬라이스로 폴백한다.
    """
    ratio_total = sum(_DIFFICULTY_RATIO.values())

    # 버킷별 기본 할당량 (내림)
    quotas: dict[RelativeRank, int] = {
        rank: k * weight // ratio_total
        for rank, weight in _DIFFICULTY_RATIO.items()
    }

    # 잉여 슬롯 배분: low → mid → high 순으로 우선 배정
    assigned = sum(quotas.values())
    fill_order = [RelativeRank.low, RelativeRank.mid, RelativeRank.high]
    for rank in fill_order:
        if assigned >= k:
            break
        quotas[rank] += 1
        assigned += 1

    # 후보를 난이도별 버킷으로 분류 (버킷 내 순서는 이미 우선순위 정렬됨)
    buckets: dict[RelativeRank, list[dict[str, Any]]] = {
        RelativeRank.low:  [],
        RelativeRank.mid:  [],
        RelativeRank.high: [],
    }
    for c in candidates:
        diff = _difficulty_for_candidate(c, learning_states)
        buckets[diff].append(c)

    selected: list[dict[str, Any]] = []
    surplus: list[dict[str, Any]] = []

    # 1차: 각 버킷을 할당량까지 채움
    for rank in fill_order:
        pool = buckets[rank]
        quota = quotas[rank]
        selected.extend(pool[:quota])
        surplus.extend(pool[quota:])  # 잉여분은 보충용으로 보관

    # 2차: 부족한 슬롯을 잉여분에서 보충 (약점 스킬 자료 우선)
    remaining = k - len(selected)
    if remaining > 0:
        surplus.sort(key=lambda c: _RANK_PRIORITY[_difficulty_for_candidate(c, learning_states)])
        selected.extend(surplus[:remaining])

    # 균형 선택 후에도 약점 스킬 자료가 앞에 오도록 다시 정렬 (spec §12-3)
    rank_index: dict[str, tuple[int, int]] = {}
    for ls in learning_states:
        priority = _RANK_PRIORITY[ls.relative_rank]
        rank_index[ls.skill.lower()] = (priority, -ls.percentile)

    def _sort_key(c: dict[str, Any]) -> tuple[int, int, float]:
        best_priority, best_pct_neg = 99, 0
        for tag in c.get("skill_tags", []):
            for skill_lower, (prio, pct_neg) in rank_index.items():
                if tag.lower() in skill_lower or skill_lower in tag.lower():
                    if prio < best_priority:
                        best_priority, best_pct_neg = prio, pct_neg
        return (best_priority, best_pct_neg, -c.get("score", 0.0))

    selected.sort(key=_sort_key)

    logger.debug(
        "[select_difficulty_balance] k=%d 할당량=%s 선택=%d",
        k,
        {r.value: q for r, q in quotas.items()},
        len(selected),
    )
    return selected[:k]


# 단계 6 — 추천 이유 생성

class _ReasonOutput(BaseModel):
    """LLM 구조화 출력 스키마 (with_structured_output에 사용)."""
    reason: str = Field(
        description="이번 추천 전체를 관통하는 전략적 이유. 사용자의 약점 스킬과 최근 활동을 구체적으로 언급해 설득력 있게 작성. 3문장 이내."
    )
    assumptions: list[str] = Field(
        description="추천 생성 시 가정한 조건 목록 (일정, 학습 강도, 목표 등). 2~3개."
    )


_SYSTEM_PROMPT = """\
당신은 KAIROS의 AI 학습 멘토입니다.
개발자의 학습 이력과 현재 역량 수준을 분석해, 지금 이 순간 가장 효과적인 학습 자료를 추천하는 것이 당신의 역할입니다.

[응답 원칙]
1. 사용자의 약점 스킬(relative_rank=low)을 반드시 언급하고, 그것을 지금 보완해야 하는 이유를 설명하세요.
2. 최근 활동 맥락을 연결해서 '지금 이 추천이 자연스러운 다음 단계'임을 보여주세요.
3. 추천 자료 제목을 1~2개 직접 언급해 구체성을 높이세요.
4. 근거 없는 칭찬이나 막연한 표현은 쓰지 마세요.
5. 반드시 한국어로 작성하세요.
6. reason은 3문장 이내, assumptions는 2~3개로 제한하세요.\
"""

_USER_TEMPLATE = """\
[사용자 프로필]
- 역할: {job_role}
- 목표 포지션: {target_positions}
- 약점 스킬 (보강 필요): {weak_skills}
- 강점 스킬 (이미 잘함): {strong_skills}

[최근 활동 요약]
{recent_summary}

[이번에 추천할 자료 목록]
{selected_titles}

위 정보를 바탕으로 추천 전략 이유와 가정 조건을 생성하세요.\
"""


async def generate_recommendation_reason(
    user: UserContext,
    learning_states: list[LearningState],
    recent_activities: list[RecentActivity],
    selected_candidates: list[dict[str, Any]],
) -> tuple[str, list[str]]:
    """
    추천 전체를 관통하는 전략적 이유와 가정 목록을 생성한다.

    반환값: (reason: str, assumptions: list[str])

    주 경로: gpt-4o with_structured_output(_ReasonOutput) — model_router RECOMMENDATION_REASON
    폴백:    API 키 없거나 호출 실패 시 규칙 기반 템플릿 사용 (LLM 호출 없음)
    """
    try:
        result = await _generate_reason_with_llm(
            user, learning_states, recent_activities, selected_candidates
        )
        return result.reason, result.assumptions

    except Exception as exc:
        logger.warning(
            "[generate_recommendation_reason] LLM 호출 실패 (%s) — 템플릿 폴백 사용",
            exc,
        )
        return _generate_reason_with_template(user, learning_states)


async def _generate_reason_with_llm(
    user: UserContext,
    learning_states: list[LearningState],
    recent_activities: list[RecentActivity],
    selected_candidates: list[dict[str, Any]],
) -> _ReasonOutput:
    """gpt-4o structured output으로 reason + assumptions를 생성한다."""
    llm = get_model(TaskType.RECOMMENDATION_REASON, temperature=0.3)
    structured_llm = llm.with_structured_output(_ReasonOutput)

    prompt = ChatPromptTemplate.from_messages([
        ("system", _SYSTEM_PROMPT),
        ("human", _USER_TEMPLATE),
    ])

    chain = prompt | structured_llm

    result: _ReasonOutput = await chain.ainvoke({
        "job_role":         user.job_role,
        "target_positions": ", ".join(user.target_positions),
        "weak_skills":      _fmt_weak_skills(learning_states),
        "strong_skills":    _fmt_strong_skills(learning_states),
        "recent_summary":   _fmt_recent_activities(recent_activities),
        "selected_titles":  _fmt_candidate_titles(selected_candidates),
    })

    logger.debug(
        "[_generate_reason_with_llm] model=%s prompt_version=%s reason_length=%d",
        get_model_name(TaskType.RECOMMENDATION_REASON),
        get_prompt_version(TaskType.RECOMMENDATION_REASON),
        len(result.reason),
    )
    return result


def _generate_reason_with_template(
    user: UserContext,
    learning_states: list[LearningState],
) -> tuple[str, list[str]]:
    """규칙 기반 폴백 — LLM 호출 없음. API 키가 없거나 호출 실패 시 사용."""
    weak_skills = [ls.skill for ls in learning_states if ls.relative_rank == RelativeRank.low]
    mid_skills  = [ls.skill for ls in learning_states if ls.relative_rank == RelativeRank.mid]

    if weak_skills:
        skill = weak_skills[0]
        reason = (
            f"현재 {skill} 역량이 상대적으로 낮은 수준입니다. "
            f"최근 활동 맥락을 이어가면서 {skill} 심화 자료를 우선 보완하는 것이 효과적입니다."
        )
    elif mid_skills:
        skill = mid_skills[0]
        reason = (
            f"{skill} 역량이 중간 수준으로, "
            f"목표 포지션({', '.join(user.target_positions)}) 대비 추가 보강이 필요합니다."
        )
    else:
        reason = (
            f"전반적인 역량 수준은 양호합니다. "
            f"목표 포지션({', '.join(user.target_positions)})의 심화 주제로 도전을 권장합니다."
        )

    assumptions = [
        "평일 저녁 학습 시간이 확보되어 있다고 가정합니다.",
        f"목표 포지션 {', '.join(user.target_positions)} 준비를 병행 중이라고 가정합니다.",
    ]
    return reason, assumptions


async def _generate_item_reason(
    candidate: dict[str, Any],
    learning_states: list[LearningState],
) -> str:
    """
    개별 추천 아이템에 대한 간결한 이유 문자열을 생성한다.

    TODO(LLM): TaskType.SIMPLE_SUMMARY gpt-4o-mini 호출로 교체 예정
    """
    skill_rank_map = {ls.skill.lower(): ls.relative_rank for ls in learning_states}
    matched_weak: list[str] = []
    matched_other: list[str] = []

    for tag in candidate.get("skill_tags", []):
        for skill_lower, rank in skill_rank_map.items():
            if tag.lower() in skill_lower or skill_lower in tag.lower():
                if rank == RelativeRank.low:
                    matched_weak.append(tag)
                else:
                    matched_other.append(tag)

    if matched_weak:
        skill_hint = matched_weak[0]
        return (
            f"{skill_hint} 역량이 현재 낮은 수준이므로, "
            f"이 자료로 기초를 탄탄히 다지는 것을 추천합니다."
        )
    if matched_other:
        skill_hint = matched_other[0]
        return f"{skill_hint} 관련 이해도를 높이는 데 도움이 되는 자료입니다."

    return "현재 학습 맥락과 연관된 자료입니다."


# 단계 7 — 최종 응답 조립

def _detect_outdated_warning(selected: list[dict[str, Any]]) -> str | None:
    """선택된 자료 중 freshness_grade가 stable이 아닌 것이 있으면 경고 문자열을 반환한다."""
    stale = [
        c["title"] for c in selected
        if c.get("freshness_grade") in ("warning", "outdated")
    ]
    if not stale:
        return None
    titles = ", ".join(f"'{t}'" for t in stale)
    return f"다음 자료는 게시 시점이 오래되어 내용이 달라졌을 수 있습니다: {titles}"


def _candidate_to_reference_item(candidate: dict[str, Any]) -> ReferenceItem:
    return ReferenceItem(
        doc_id=candidate["doc_id"],
        title=candidate["title"],
        url=candidate["url"],
        freshness=FreshnessGrade(candidate.get("freshness_grade", "stable")),
        skill_tags=candidate.get("skill_tags", []),
        source_type=candidate.get("source_type", "unknown"),
    )


def _difficulty_for_candidate(
    candidate: dict[str, Any],
    learning_states: list[LearningState],
) -> RelativeRank:
    """가장 약한 매칭 learning_state를 기준으로 난이도 레이블을 반환한다."""
    for tag in candidate.get("skill_tags", []):
        for ls in learning_states:
            if tag.lower() in ls.skill.lower() or ls.skill.lower() in tag.lower():
                return _RANK_TO_DIFFICULTY[ls.relative_rank]
    return RelativeRank.mid


async def _assemble_response(
    request: RecommendationRequest,
    selected: list[dict[str, Any]],
    reason: str,
    assumptions: list[str],
) -> RecommendationResponse:
    """정렬된 후보 목록으로 최종 RecommendationResponse를 조립한다."""
    items: list[RecommendationItem] = []
    for candidate in selected:
        item_reason = await _generate_item_reason(candidate, request.learning_states)
        items.append(
            RecommendationItem(
                type=RecommendationRequestType.reference,
                title=candidate["title"],
                reason=item_reason,
                difficulty=_difficulty_for_candidate(candidate, request.learning_states),
                estimated_time=_estimate_reading_time(candidate),
                doc_id=candidate["doc_id"],
            )
        )

    references = [_candidate_to_reference_item(c) for c in selected]
    outdated_warning = _detect_outdated_warning(selected)

    return RecommendationResponse(
        items=items,
        reason=reason,
        assumptions=assumptions,
        references=references,
        outdated_warning=outdated_warning,
        model_meta=ModelMeta(
            model=get_model_name(TaskType.RECOMMENDATION_REASON),
            prompt_version=get_prompt_version(TaskType.RECOMMENDATION_REASON),
            policy_version=request.policy_version,
        ),
    )


# 공개 엔트리 포인트

async def run_recommendation(request: RecommendationRequest) -> RecommendationResponse:
    """
    POST /internal/recommendations의 메인 엔트리 포인트.

    각 단계는 독립적으로 교체 가능하며, 전체 RecommendationResponse를 반환한다.
    """
    logger.info(
        "[run_recommendation] request_id=%s user_id=%s",
        request.request_id,
        request.user.user_id,
    )

    # 1. 후보 자료 조회 (목 → Qdrant)
    candidates = retrieve_candidates(request)

    # 2. 이미 본 자료 제거
    candidates = _filter_history_exclusions(candidates, request.history_exclusions)

    # 3. 스킬 관련 자료만 유지
    candidates = _filter_by_skill_relevance(
        candidates, request.learning_states, request.recent_activities
    )

    # 4. 약점 스킬 / 낮은 percentile 우선 정렬
    candidates = _rank_by_learning_priority(candidates, request.learning_states)

    # 5. 1:2:1 (high:mid:low) 난이도 균형 선택
    candidates = _select_with_difficulty_balance(
        candidates, request.learning_states, TOP_K
    )

    if not candidates:
        logger.warning(
            "[run_recommendation] 필터링 후 후보 없음 — 목 데이터 상위 결과로 폴백"
        )
        # 폴백: 스킬 매칭 없이 점수 높은 자료 반환
        # TODO(Qdrant 폴백): 큐레이션 레퍼런스 쿼리로 교체. AI Tech Design v3, §14 참고
        candidates = _select_top_k(_load_mock_data()["mock_qdrant_hits"], TOP_K)

    # 6. 전체 추천 이유 생성
    reason, assumptions = await generate_recommendation_reason(
        user=request.user,
        learning_states=request.learning_states,
        recent_activities=request.recent_activities,
        selected_candidates=candidates,
    )

    # 7. 응답 조립 및 반환
    response = await _assemble_response(request, candidates, reason, assumptions)

    logger.info(
        "[run_recommendation] 완료 — %d개 반환, outdated_warning=%s",
        len(response.items),
        bool(response.outdated_warning),
    )
    return response


# 내부 헬퍼

def _load_mock_data() -> dict[str, Any]:
    """tests/mock_recommendation_data.json에서 목 Qdrant 데이터를 로드한다."""
    with MOCK_DATA_PATH.open(encoding="utf-8") as f:
        return json.load(f)


def _estimate_reading_time(candidate: dict[str, Any]) -> str:
    """source_type 기반 예상 읽기 시간 추정. TODO: Qdrant payload에 word count 포함 시 교체."""
    source_type = candidate.get("source_type", "unknown")
    time_map = {
        "official_doc": "30~40분",
        "tech_blog":    "15~20분",
        "curated":      "20~30분",
    }
    return time_map.get(source_type, "20분")


def _fmt_weak_skills(learning_states: list[LearningState]) -> str:
    """LLM 프롬프트 주입용 약점 스킬 포매팅."""
    weak = [ls for ls in learning_states if ls.relative_rank == RelativeRank.low]
    if not weak:
        return "없음"
    return ", ".join(f"{ls.skill}(percentile {ls.percentile}%)" for ls in weak)


def _fmt_strong_skills(learning_states: list[LearningState]) -> str:
    """LLM 프롬프트 주입용 강점 스킬 포매팅."""
    strong = [ls for ls in learning_states if ls.relative_rank == RelativeRank.high]
    if not strong:
        return "없음"
    return ", ".join(f"{ls.skill}(percentile {ls.percentile}%)" for ls in strong)


def _fmt_recent_activities(recent_activities: list[RecentActivity]) -> str:
    """LLM 프롬프트 주입용 최근 활동 포매팅 (최대 5개, 토큰 절약)."""
    if not recent_activities:
        return "최근 활동 없음"
    return "\n".join(
        f"- [{act.source.value}/{act.category.value}] {act.summary}"
        for act in recent_activities[:5]
    )


def _fmt_candidate_titles(candidates: list[dict[str, Any]]) -> str:
    """LLM 프롬프트 주입용 선택 후보 제목 포매팅."""
    return "\n".join(
        f"- {c['title']} ({', '.join(c.get('skill_tags', []))})"
        for c in candidates
    )

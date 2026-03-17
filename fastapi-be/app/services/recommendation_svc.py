"""
Recommendation service — POST /internal/recommendations

Execution flow:
    run_recommendation(request)
        │
        ├─ 1. retrieve_candidates()          ← TODO: replace mock with Qdrant
        ├─ 2. _filter_history_exclusions()   ← drop already-seen doc_ids
        ├─ 3. _filter_by_skill_relevance()   ← keep only skill-matched candidates
        ├─ 4. _rank_by_learning_priority()   ← low-rank skills surface first (spec §12-3)
        ├─ 5. _select_top_k()               ← cap result count
        ├─ 6. generate_recommendation_reason()  ← gpt-4o LLM chain (template fallback)
        └─ 7. _assemble_response()           ← build RecommendationResponse

Ownership rules (AI Tech Spec v3):
  - This service must NOT query MySQL directly.
  - All domain context arrives via RecommendationRequest (assembled by Spring Boot).
  - Qdrant is the only external store this service reads from.
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

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

TOP_K = 5                          # max references returned per request
MOCK_DATA_PATH = Path(__file__).parent.parent.parent / "tests" / "mock_recommendation_data.json"

# Difficulty mapping: relative_rank of the weakest skill → item difficulty label
_RANK_TO_DIFFICULTY: dict[RelativeRank, RelativeRank] = {
    RelativeRank.low:  RelativeRank.low,   # weak skill → introductory material
    RelativeRank.mid:  RelativeRank.mid,
    RelativeRank.high: RelativeRank.high,  # strong skill → advanced material
}

# Priority weight for ranking: lower relative_rank = higher recommendation priority
_RANK_PRIORITY: dict[RelativeRank, int] = {
    RelativeRank.low:  0,   # highest priority — fill the gap first
    RelativeRank.mid:  1,
    RelativeRank.high: 2,
}

# Difficulty composition target ratio for TOP_K results: high : mid : low = 1 : 2 : 1
# For TOP_K=5, the extra slot goes to low (highest learning priority).
# Spring Boot assigns relative_rank using 30 / 40 / 30 percentile thresholds:
#   bottom 30 % → low, middle 40 % → mid, top 30 % → high
_DIFFICULTY_RATIO: dict[RelativeRank, int] = {
    RelativeRank.low:  1,
    RelativeRank.mid:  2,
    RelativeRank.high: 1,
}


# ---------------------------------------------------------------------------
# Step 1 — Retrieve candidates
# ---------------------------------------------------------------------------

def retrieve_candidates(request: RecommendationRequest) -> list[dict[str, Any]]:
    """
    Return a list of raw candidate documents for the given request context.

    Current implementation: loads mock data from tests/mock_recommendation_data.json.
    In production, replace the body of this function with a Qdrant Self-Querying RAG call.

    TODO (Qdrant integration):
        1. Build a query string from learning_states + recent_activities:
               query_text = _build_qdrant_query(request)
        2. Build a metadata filter from target_positions + skill tags:
               filters = _build_qdrant_filter(request)
        3. Call qdrant_client.search():
               hits = qdrant_client.search(
                   collection_name="reference_chunks",
                   query_vector=embed(query_text),
                   query_filter=filters,
                   limit=TOP_K * 3,   # over-fetch before filtering
                   with_payload=True,
               )
        4. Return [hit.payload for hit in hits]

    See spec: AI Tech Design v3, §11 (Qdrant Design), §5-2 (Recommendation Pipeline)
    """
    logger.info(
        "[retrieve_candidates] MOCK MODE — loading from %s", MOCK_DATA_PATH
    )
    mock_data = _load_mock_data()
    return mock_data["mock_qdrant_hits"]


# ---------------------------------------------------------------------------
# Step 2 — Filter: remove already-seen documents
# ---------------------------------------------------------------------------

def _filter_history_exclusions(
    candidates: list[dict[str, Any]],
    exclusions: list[str],
) -> list[dict[str, Any]]:
    """
    Drop candidates whose doc_id appears in history_exclusions.
    Prevents re-recommending documents the user has already received.
    """
    exclusion_set = set(exclusions)
    filtered = [c for c in candidates if c["doc_id"] not in exclusion_set]
    logger.debug(
        "[filter_exclusions] %d → %d candidates (dropped %d)",
        len(candidates), len(filtered), len(candidates) - len(filtered),
    )
    return filtered


# ---------------------------------------------------------------------------
# Step 3 — Filter: keep only skill-relevant documents
# ---------------------------------------------------------------------------

def _filter_by_skill_relevance(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
    recent_activities: list[RecentActivity],
) -> list[dict[str, Any]]:
    """
    Keep candidates that overlap with the user's skill universe.

    Skill universe = skills from learning_states + tech_stacks from recent_activities.
    A candidate is relevant if any of its skill_tags appears in the skill universe
    (case-insensitive substring match to handle e.g. "Spring Security" ↔ "Spring").

    TODO (Qdrant integration):
        With real Qdrant this filter can be pushed down as a payload filter,
        making this Python-side filter redundant. Keep it as a safety net.
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
        "[filter_skill] %d → %d candidates (skill universe size: %d)",
        len(candidates), len(filtered), len(skill_universe),
    )
    return filtered


# ---------------------------------------------------------------------------
# Step 4 — Rank: surface weakest-skill materials first
# ---------------------------------------------------------------------------

def _rank_by_learning_priority(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
) -> list[dict[str, Any]]:
    """
    Sort candidates so that documents covering weak skills appear first.

    Priority logic (spec §12-3 Hybrid 2+3 approach):
      - Primary:   lowest relative_rank skill match (low → mid → high)
      - Secondary: lowest percentile (most room for improvement)
      - Tertiary:  Qdrant similarity score (already baked into mock data)

    Candidates with no matching skill_tag are sorted last.
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


# ---------------------------------------------------------------------------
# Step 5 — Select top-K with difficulty balance
# ---------------------------------------------------------------------------

def _select_top_k(
    candidates: list[dict[str, Any]],
    k: int = TOP_K,
) -> list[dict[str, Any]]:
    """Simple top-K slice. Used only in the empty-result fallback path."""
    return candidates[:k]


def _select_with_difficulty_balance(
    candidates: list[dict[str, Any]],
    learning_states: list[LearningState],
    k: int = TOP_K,
) -> list[dict[str, Any]]:
    """
    Select k candidates while enforcing the 1:2:1 difficulty composition
    (high : mid : low, defined in _DIFFICULTY_RATIO).

    Algorithm:
      1. Bucket each candidate by derived difficulty (preserving rank order within
         each bucket — weakest skill first, as established by _rank_by_learning_priority).
      2. Compute per-bucket quotas proportional to _DIFFICULTY_RATIO.
         The extra slot(s) from rounding are awarded to low first, then mid
         (lower rank = higher learning priority).
      3. Fill each bucket up to its quota; any bucket short of quota donates
         its remainder to the next-priority bucket.

    Falls back to a simple top-K slice if all candidates share the same
    difficulty (e.g., all-high scenario), preserving existing behaviour.
    """
    ratio_total = sum(_DIFFICULTY_RATIO.values())

    # Compute base quotas (floor division)
    quotas: dict[RelativeRank, int] = {
        rank: k * weight // ratio_total
        for rank, weight in _DIFFICULTY_RATIO.items()
    }

    # Distribute leftover slots: prioritise low → mid → high
    assigned = sum(quotas.values())
    fill_order = [RelativeRank.low, RelativeRank.mid, RelativeRank.high]
    for rank in fill_order:
        if assigned >= k:
            break
        quotas[rank] += 1
        assigned += 1

    # Bucket candidates (order within each bucket is already priority-sorted)
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

    # First pass: fill each bucket up to its quota
    for rank in fill_order:
        pool = buckets[rank]
        quota = quotas[rank]
        selected.extend(pool[:quota])
        surplus.extend(pool[quota:])   # leftovers available for gap-filling

    # Second pass: fill remaining slots from surplus (weak-skill items first)
    remaining = k - len(selected)
    if remaining > 0:
        surplus.sort(key=lambda c: _RANK_PRIORITY[_difficulty_for_candidate(c, learning_states)])
        selected.extend(surplus[:remaining])

    # Restore weak-skill-first order within the balanced selection so that
    # low-rank items still surface before mid/high (spec §12-3).
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
        "[select_difficulty_balance] k=%d quotas=%s selected=%d",
        k,
        {r.value: q for r, q in quotas.items()},
        len(selected),
    )
    return selected[:k]


# ---------------------------------------------------------------------------
# Step 6 — Reason generation  ← THE KEY FUNCTION
# ---------------------------------------------------------------------------

# ── Structured output schema for the LLM ───────────────────────────────────
class _ReasonOutput(BaseModel):
    """LLM must respond with this exact structure (used with with_structured_output)."""
    reason: str = Field(
        description="이번 추천 전체를 관통하는 전략적 이유. 사용자의 약점 스킬과 최근 활동을 구체적으로 언급해 설득력 있게 작성. 3문장 이내."
    )
    assumptions: list[str] = Field(
        description="추천 생성 시 가정한 조건 목록 (일정, 학습 강도, 목표 등). 2~3개."
    )


# ── System prompt ───────────────────────────────────────────────────────────
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

# ── User prompt template ────────────────────────────────────────────────────
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
    Generate the overall strategic recommendation reason and assumption list.

    Returns:
        (reason: str, assumptions: list[str])

    Primary:  gpt-4o via with_structured_output(_ReasonOutput)
              → model_router TaskType.RECOMMENDATION_REASON
    Fallback: rule-based template (no LLM call) if API key is missing or call fails
    """
    try:
        result = await _generate_reason_with_llm(
            user, learning_states, recent_activities, selected_candidates
        )
        return result.reason, result.assumptions

    except Exception as exc:
        logger.warning(
            "[generate_recommendation_reason] LLM call failed (%s) — using template fallback",
            exc,
        )
        return _generate_reason_with_template(user, learning_states)


async def _generate_reason_with_llm(
    user: UserContext,
    learning_states: list[LearningState],
    recent_activities: list[RecentActivity],
    selected_candidates: list[dict[str, Any]],
) -> _ReasonOutput:
    """
    Call gpt-4o with structured output to generate reason + assumptions.

    Uses LangChain with_structured_output to enforce the _ReasonOutput Pydantic schema.
    Model: gpt-4o (TaskType.RECOMMENDATION_REASON)
    Prompt version: tracked via get_prompt_version()
    """
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
    """
    Rule-based fallback — no LLM call.
    Used when OPENAI_API_KEY is not set or the LLM call fails.
    """
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
    Generate a concise reason string for a single recommendation item.

    TODO(LLM): Replace rule-based logic with a lightweight gpt-4o-mini call
    using TaskType.SIMPLE_SUMMARY once the prompt template is ready.
    """
    # Find the weakest matching skill for this candidate
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


# ---------------------------------------------------------------------------
# Step 7 — Assemble final response
# ---------------------------------------------------------------------------

def _detect_outdated_warning(selected: list[dict[str, Any]]) -> str | None:
    """Return a warning string if any selected candidate has a non-stable freshness grade."""
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
    """Derive difficulty label from the weakest matching learning state."""
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
    """Build the final RecommendationResponse from ranked candidates."""
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


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

async def run_recommendation(request: RecommendationRequest) -> RecommendationResponse:
    """
    Main entry point for POST /internal/recommendations.

    Orchestrates all steps and returns a fully populated RecommendationResponse.
    Each step is independently replaceable without touching the others.
    """
    logger.info(
        "[run_recommendation] request_id=%s user_id=%s",
        request.request_id,
        request.user.user_id,
    )

    # 1. Retrieve candidates (mock → Qdrant)
    candidates = retrieve_candidates(request)

    # 2. Remove already-seen documents
    candidates = _filter_history_exclusions(candidates, request.history_exclusions)

    # 3. Keep only skill-relevant documents
    candidates = _filter_by_skill_relevance(
        candidates, request.learning_states, request.recent_activities
    )

    # 4. Sort: weakest skill / lowest percentile first
    candidates = _rank_by_learning_priority(candidates, request.learning_states)

    # 5. Select with 1:2:1 (high:mid:low) difficulty balance
    candidates = _select_with_difficulty_balance(
        candidates, request.learning_states, TOP_K
    )

    if not candidates:
        logger.warning(
            "[run_recommendation] No candidates after filtering — falling back to top mock hits"
        )
        # Fallback: return the highest-scoring mock docs regardless of skill match
        # TODO(Qdrant fallback): replace with curated reference set query
        #   See spec: AI Tech Design v3, §14 (fallback table — "Qdrant 검색 실패")
        candidates = _select_top_k(_load_mock_data()["mock_qdrant_hits"], TOP_K)

    # 6. Generate overall recommendation reason
    reason, assumptions = await generate_recommendation_reason(
        user=request.user,
        learning_states=request.learning_states,
        recent_activities=request.recent_activities,
        selected_candidates=candidates,
    )

    # 7. Assemble and return the response
    response = await _assemble_response(request, candidates, reason, assumptions)

    logger.info(
        "[run_recommendation] done — %d items returned, outdated_warning=%s",
        len(response.items),
        bool(response.outdated_warning),
    )
    return response


# ---------------------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------------------

def _load_mock_data() -> dict[str, Any]:
    """Load and return the mock Qdrant data from tests/mock_recommendation_data.json."""
    with MOCK_DATA_PATH.open(encoding="utf-8") as f:
        return json.load(f)


def _estimate_reading_time(candidate: dict[str, Any]) -> str:
    """
    Rough reading-time heuristic based on source_type.

    TODO: replace with actual document length once Qdrant payloads include word count.
    """
    source_type = candidate.get("source_type", "unknown")
    time_map = {
        "official_doc": "30~40분",
        "tech_blog":    "15~20분",
        "curated":      "20~30분",
    }
    return time_map.get(source_type, "20분")


def _fmt_weak_skills(learning_states: list[LearningState]) -> str:
    """Format weak skills for LLM prompt injection (used in TODO block)."""
    weak = [ls for ls in learning_states if ls.relative_rank == RelativeRank.low]
    if not weak:
        return "없음"
    return ", ".join(f"{ls.skill}(percentile {ls.percentile}%)" for ls in weak)


def _fmt_strong_skills(learning_states: list[LearningState]) -> str:
    """Format strong skills for LLM prompt injection (used in TODO block)."""
    strong = [ls for ls in learning_states if ls.relative_rank == RelativeRank.high]
    if not strong:
        return "없음"
    return ", ".join(f"{ls.skill}(percentile {ls.percentile}%)" for ls in strong)


def _fmt_recent_activities(recent_activities: list[RecentActivity]) -> str:
    """Format recent activities for LLM prompt injection (used in TODO block)."""
    if not recent_activities:
        return "최근 활동 없음"
    return "\n".join(
        f"- [{act.source.value}/{act.category.value}] {act.summary}"
        for act in recent_activities[:5]   # cap at 5 to limit token usage
    )


def _fmt_candidate_titles(candidates: list[dict[str, Any]]) -> str:
    """Format selected candidate titles for LLM prompt injection (used in TODO block)."""
    return "\n".join(
        f"- {c['title']} ({', '.join(c.get('skill_tags', []))})"
        for c in candidates
    )

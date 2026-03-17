"""
추천 품질 메트릭 테스트

측정 항목:
    1. relevance          — 추천 아이템의 skill_tags ↔ 사용자 skill universe 겹침 비율
    2. difficulty_align   — weak skill → low/mid 난이도, strong skill → high 난이도 정합성
    3. reason_specificity — LLM 생성 reason에 스킬명 / 포지션 명시 여부
    4. weak_first_order   — low rank 스킬 관련 자료가 앞에 오는지 (랭킹 로직 검증)

시나리오:
    A. all_low   — 모든 스킬이 약점
    B. mixed     — junior backend (JWT↓, Spring Security↔, Spring Boot↑)
    C. all_high  — 모든 스킬이 강점

실행 방법:
    python -m pytest tests/test_recommendation_quality.py -s -v
    python tests/test_recommendation_quality.py
"""

import asyncio
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

# GMS 엔드포인트 설정 — 서비스 import 전에 세팅해야 model_router가 키를 읽음
os.environ.setdefault("OPENAI_API_KEY",  "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246")
os.environ.setdefault("OPENAI_API_BASE", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")

from app.models.schemas import (
    Category,
    LearningState,
    RecentActivity,
    RecommendationRequest,
    RelativeRank,
    Source,
    SurveyContext,
    UserContext,
)
from app.services import recommendation_svc

RESULTS_DIR = Path(__file__).parent / "results"


# ---------------------------------------------------------------------------
# 시나리오 정의
# ---------------------------------------------------------------------------

def _user(target_positions: list[str]) -> UserContext:
    return UserContext(
        user_id=99,
        job_role="취준생",
        target_positions=target_positions,
        survey_context=SurveyContext(
            job_role="취준생",
            target_positions=target_positions,
            tech_stacks=["Spring Boot", "JWT", "MySQL"],
            persona="default",
        ),
    )


SCENARIOS: dict[str, RecommendationRequest] = {
    "all_low": RecommendationRequest(
        request_id="quality_all_low",
        trace_id="trace_q1",
        user=_user(["backend"]),
        learning_states=[
            LearningState(skill="JWT",            raw_score=1.0, relative_rank=RelativeRank.low, percentile=20),
            LearningState(skill="Spring Security", raw_score=1.1, relative_rank=RelativeRank.low, percentile=25),
            LearningState(skill="MySQL",           raw_score=0.9, relative_rank=RelativeRank.low, percentile=18),
        ],
        recent_activities=[
            RecentActivity(source=Source.github, category=Category.development,
                           tech_stacks=["Spring Boot", "JWT"],
                           summary="JWT 인증 필터 추가"),
        ],
        history_exclusions=[],
    ),
    "mixed": RecommendationRequest(
        request_id="quality_mixed",
        trace_id="trace_q2",
        user=_user(["backend"]),
        learning_states=[
            LearningState(skill="Spring Security", raw_score=1.8, relative_rank=RelativeRank.mid,  percentile=42),
            LearningState(skill="JWT",             raw_score=1.2, relative_rank=RelativeRank.low,  percentile=28),
            LearningState(skill="Spring Boot",     raw_score=2.6, relative_rank=RelativeRank.high, percentile=65),
            LearningState(skill="MySQL",           raw_score=1.5, relative_rank=RelativeRank.mid,  percentile=38),
        ],
        recent_activities=[
            RecentActivity(source=Source.velog, category=Category.learning,
                           tech_stacks=["Spring Security", "OAuth"],
                           summary="OAuth 로그인 흐름과 Spring Security 필터 체인 정리"),
            RecentActivity(source=Source.github, category=Category.development,
                           tech_stacks=["Spring Boot", "JWT"],
                           summary="JWT 기반 인증 필터 클래스 추가"),
        ],
        history_exclusions=["ref_oauth2_old_001"],
    ),
    "all_high": RecommendationRequest(
        request_id="quality_all_high",
        trace_id="trace_q3",
        user=_user(["backend", "fullstack"]),
        learning_states=[
            LearningState(skill="Spring Security", raw_score=3.5, relative_rank=RelativeRank.high, percentile=80),
            LearningState(skill="JWT",             raw_score=3.2, relative_rank=RelativeRank.high, percentile=75),
            LearningState(skill="Spring Boot",     raw_score=3.8, relative_rank=RelativeRank.high, percentile=85),
        ],
        recent_activities=[
            RecentActivity(source=Source.github, category=Category.development,
                           tech_stacks=["Spring Security", "JWT"],
                           summary="Spring Security 커스텀 필터 체인 최적화"),
        ],
        history_exclusions=[],
    ),
}


# ---------------------------------------------------------------------------
# 품질 메트릭 함수
# ---------------------------------------------------------------------------

def measure_relevance(response, request: RecommendationRequest) -> dict:
    """
    추천 아이템의 skill_tags ↔ 사용자 skill universe 겹침 비율.
    skill_universe = learning_states.skill + recent_activities.tech_stacks
    기준: ≥ 60% (5개 중 3개)
    """
    skill_universe = set()
    for ls in request.learning_states:
        skill_universe.add(ls.skill.lower())
    for act in request.recent_activities:
        for t in act.tech_stacks:
            skill_universe.add(t.lower())

    relevant = sum(
        1 for ref in response.references
        if any(
            any(tag.lower() in su or su in tag.lower() for su in skill_universe)
            for tag in ref.skill_tags
        )
    )
    total = len(response.references)
    rate  = relevant / total if total > 0 else 0.0
    return {
        "relevant_items": relevant,
        "total_items":    total,
        "overlap_rate":   round(rate, 3),
        "pass":           rate >= 0.6,
    }


def measure_difficulty_alignment(response, request: RecommendationRequest) -> dict:
    """
    약점 스킬(low) → low/mid 난이도, 강점 스킬(high) → mid/high 난이도 정합성.
    판단 기준: 문서 추천의 '주 목적 스킬' = skill_tags 중 rank가 가장 낮은(약한) 스킬.
    mismatch 조건:
        - weakest matching rank = high 인데 difficulty=low
        - weakest matching rank = low 인데 difficulty=high
    기준: mismatch_rate < 20%
    """
    rank_priority = {RelativeRank.low: 0, RelativeRank.mid: 1, RelativeRank.high: 2}
    rank_map      = {ls.skill.lower(): ls.relative_rank for ls in request.learning_states}

    mismatches = 0
    for item in response.items:
        ref = next((r for r in response.references if r.doc_id == item.doc_id), None)
        if ref is None:
            continue

        # skill_tags 중 가장 약한(낮은 rank priority) 스킬을 추천 주목적으로 판단
        weakest_rank = None
        for tag in ref.skill_tags:
            for skill_lower, rank in rank_map.items():
                if tag.lower() in skill_lower or skill_lower in tag.lower():
                    if weakest_rank is None or rank_priority[rank] < rank_priority[weakest_rank]:
                        weakest_rank = rank

        if weakest_rank is None:
            continue
        if weakest_rank == RelativeRank.high and item.difficulty == RelativeRank.low:
            mismatches += 1
        elif weakest_rank == RelativeRank.low and item.difficulty == RelativeRank.high:
            mismatches += 1

    total         = len(response.items)
    mismatch_rate = mismatches / total if total > 0 else 0.0
    return {
        "mismatches":    mismatches,
        "total_items":   total,
        "mismatch_rate": round(mismatch_rate, 3),
        "pass":          mismatch_rate < 0.2,
    }


def measure_reason_specificity(response, request: RecommendationRequest) -> dict:
    """
    전체 reason에 스킬명 / 포지션 언급 여부 + 개별 item reason에 스킬명 포함 비율.
    기준: overall reason에 스킬명 ≥ 1개 + item specificity_rate ≥ 40%
    """
    reason      = response.reason.lower()
    skill_names = [ls.skill.lower() for ls in request.learning_states]
    positions   = [p.lower() for p in request.user.target_positions]

    skill_mentions    = sum(1 for s in skill_names if s in reason)
    position_mentions = sum(1 for p in positions   if p in reason)

    item_specific = sum(
        1 for item in response.items
        if any(s in item.reason.lower() for s in skill_names)
    )
    item_rate = item_specific / len(response.items) if response.items else 0.0

    return {
        "overall_skill_mentions":    skill_mentions,
        "overall_position_mentions": position_mentions,
        "item_specific_count":       item_specific,
        "item_specificity_rate":     round(item_rate, 3),
        "pass":                      skill_mentions >= 1 and item_rate >= 0.4,
    }


def measure_weak_first_order(response, request: RecommendationRequest) -> dict:
    """
    low rank 스킬 관련 자료가 high rank 자료보다 앞에 위치하는지.
    우선순위: low=0, mid=1, high=2 → 오름차순이면 정렬 올바름.
    기준: inversion 0
    """
    rank_priority = {RelativeRank.low: 0, RelativeRank.mid: 1, RelativeRank.high: 2}
    rank_map      = {ls.skill.lower(): ls.relative_rank for ls in request.learning_states}

    priorities = []
    for item in response.items:
        ref = next((r for r in response.references if r.doc_id == item.doc_id), None)
        best = 99
        if ref:
            for tag in ref.skill_tags:
                for skill_lower, rank in rank_map.items():
                    if tag.lower() in skill_lower or skill_lower in tag.lower():
                        p = rank_priority[rank]
                        if p < best:
                            best = p
        priorities.append(best)

    inversions = sum(
        1 for i in range(len(priorities) - 1)
        if priorities[i] > priorities[i + 1]
    )
    return {
        "item_priorities": priorities,
        "inversions":      inversions,
        "pass":            inversions == 0,
    }


# ---------------------------------------------------------------------------
# 테스트 실행기
# ---------------------------------------------------------------------------

async def run_quality_tests() -> dict:
    RESULTS_DIR.mkdir(exist_ok=True)
    all_results: dict = {}

    for scenario_name, request in SCENARIOS.items():
        print(f"\n{'=' * 60}")
        print(f"시나리오: {scenario_name}")
        print("=" * 60)

        response = await recommendation_svc.run_recommendation(request)

        print(f"반환 아이템 {len(response.items)}개:")
        for item in response.items:
            print(f"  - {item.title[:52]}  difficulty={item.difficulty.value}")

        metrics = {
            "relevance":          measure_relevance(response, request),
            "difficulty_align":   measure_difficulty_alignment(response, request),
            "reason_specificity": measure_reason_specificity(response, request),
            "weak_first_order":   measure_weak_first_order(response, request),
        }

        print("\n품질 메트릭:")
        all_passed = True
        for name, result in metrics.items():
            flag = "PASS" if result["pass"] else "FAIL"
            if not result["pass"]:
                all_passed = False
            print(f"  [{flag}] {name:25s} {result}")

        print(f"\n종합: {'ALL PASS' if all_passed else 'HAS FAILURES'}")

        all_results[scenario_name] = {
            "items":      [item.model_dump() for item in response.items],
            "reason":     response.reason,
            "assumptions": response.assumptions,
            "metrics":    metrics,
            "all_passed": all_passed,
        }

    out_path = RESULTS_DIR / "recommendation_quality.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2)
    print(f"\n결과 저장 → {out_path}")
    return all_results


# ---------------------------------------------------------------------------
# pytest entry points
# ---------------------------------------------------------------------------

def test_recommendation_quality_mixed():
    """pytest: mixed 시나리오 — 가장 현실적인 케이스."""
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    request  = SCENARIOS["mixed"]
    response = asyncio.run(recommendation_svc.run_recommendation(request))

    relevance = measure_relevance(response, request)
    assert relevance["pass"], f"relevance FAIL: {relevance}"

    ordering = measure_weak_first_order(response, request)
    assert ordering["pass"], f"weak_first_order FAIL: {ordering}"

    difficulty = measure_difficulty_alignment(response, request)
    assert difficulty["pass"], f"difficulty_align FAIL: {difficulty}"


def test_recommendation_quality_all_low():
    """pytest: all_low 시나리오 — 약점만 있는 경우 관련성 100% 기대."""
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    request  = SCENARIOS["all_low"]
    response = asyncio.run(recommendation_svc.run_recommendation(request))

    relevance = measure_relevance(response, request)
    assert relevance["pass"], f"relevance FAIL: {relevance}"


# ---------------------------------------------------------------------------
# standalone entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(run_quality_tests())

"""
E2E 성능 테스트: profile_analyzer → recommendation → quiz (Scenario A)

실행 방법:
    python -m pytest tests/test_e2e_profile_to_quiz.py -s -v
    python tests/test_e2e_profile_to_quiz.py              # 첫 실행 (profile 분석 포함)
    python tests/test_e2e_profile_to_quiz.py --cache      # 캐시 재사용 (profile 생략)

측정 항목:
    - profile_analyzer 전체 소요시간 (데이터 수집 / LLM 분석 / 최종 요약)
    - recommendation 소요시간
    - quiz 소요시간
    - hot path (recommendation + quiz) 합산 시간
"""

import asyncio
import json
import os
import sys
import time
from pathlib import Path

# 프로젝트 루트를 sys.path에 추가
sys.path.insert(0, str(Path(__file__).parent.parent))

# GMS 엔드포인트 설정 — 서비스 import 전에 세팅해야 model_router가 키를 읽음
os.environ.setdefault("OPENAI_API_KEY",  "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246")
os.environ.setdefault("OPENAI_API_BASE", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")

from app.models.schemas import (
    Category,
    LearningState,
    QuizRequest,
    QuizType,
    RecentActivity,
    RecommendationHint,
    RecommendationRequest,
    RelativeRank,
    Source,
    SurveyContext,
    UserContext,
)
from app.services import quiz_svc, recommendation_svc
from app.services.profile_analyzer import run_integrated_analysis

FIXTURES_DIR = Path(__file__).parent / "fixtures"
RESULTS_DIR  = Path(__file__).parent / "results"
PROFILE_CACHE = FIXTURES_DIR / "sample_final_profile.json"

# 목표 latency 기준 (초)
THRESHOLDS = {
    "profile_total":  60.0,
    "recommendation":  5.0,
    "quiz":            8.0,
}


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

async def _measure(name: str, coro):
    t0 = time.perf_counter()
    result = await coro
    elapsed = time.perf_counter() - t0
    return result, elapsed


def _derive_learning_states(skill_frequency: dict[str, int], max_skills: int = 8) -> list[LearningState]:
    """
    skill_frequency 기반으로 LearningState 파생 — 30 / 40 / 30 percentile 기준.

    전체 스킬의 빈도 분포에서 각 스킬의 상대 위치를 계산한 뒤:
      - percentile > 70  → high  (상위 30%: 자주 사용 = 이미 익숙한 스킬)
      - 30 < percentile ≤ 70 → mid
      - percentile ≤ 30  → low   (하위 30%: 거의 사용 안 함 = 보완 필요)

    추천 다양성을 위해 low / mid / high 각 구간에서 최대 max_skills/3 개씩 선택.
    스킬이 부족한 구간은 다른 구간에서 보충.
    """
    if not skill_frequency:
        return []

    all_freqs = sorted(skill_frequency.values())
    n = len(all_freqs)

    def _percentile(freq: int) -> int:
        """해당 빈도가 전체 분포에서 차지하는 백분위 (높을수록 자주 사용)."""
        below = sum(1 for f in all_freqs if f < freq)
        return int(below / n * 100)

    def _rank(pct: int) -> RelativeRank:
        if pct > 70:
            return RelativeRank.high
        elif pct > 30:
            return RelativeRank.mid
        else:
            return RelativeRank.low

    # 전체 스킬에 rank / percentile 부여
    annotated = [
        (skill, freq, _percentile(freq), _rank(_percentile(freq)))
        for skill, freq in skill_frequency.items()
    ]

    # 각 구간별로 빈도 내림차순 정렬 (추천 관련성 높은 순)
    buckets: dict[RelativeRank, list] = {r: [] for r in RelativeRank}
    for entry in annotated:
        buckets[entry[3]].append(entry)
    for r in buckets:
        buckets[r].sort(key=lambda x: -x[1])

    # 구간별 할당: max_skills 개를 low:mid:high = 1:2:1 로 분배
    # (추천 로직과 동일한 비율 — 약점 구간에서 더 많이 선택)
    per_bucket = max(1, max_skills // 4)
    targets = {
        RelativeRank.low:  per_bucket,
        RelativeRank.mid:  per_bucket * 2,
        RelativeRank.high: per_bucket,
    }

    selected: list[LearningState] = []
    surplus: list = []
    for rank in [RelativeRank.low, RelativeRank.mid, RelativeRank.high]:
        pool = buckets[rank]
        quota = targets[rank]
        for skill, freq, pct, r in pool[:quota]:
            selected.append(LearningState(
                skill=skill,
                raw_score=float(freq) / 10,
                relative_rank=r,
                percentile=pct,
            ))
        surplus.extend(pool[quota:])

    # 남은 슬롯은 빈도 높은 순으로 채움
    remaining = max_skills - len(selected)
    if remaining > 0:
        surplus.sort(key=lambda x: -x[1])
        for skill, freq, pct, r in surplus[:remaining]:
            selected.append(LearningState(
                skill=skill,
                raw_score=float(freq) / 10,
                relative_rank=r,
                percentile=pct,
            ))

    return selected[:max_skills]


def _derive_recent_activities(activities: list[dict]) -> list[RecentActivity]:
    """profile_analyzer 활동 내역 → RecentActivity 변환 (최대 3개)"""
    category_map = {"개발": Category.development, "학습": Category.learning}
    result = []
    for act in activities[:3]:
        raw_category = act.get("category", "기타")
        category = category_map.get(raw_category, Category.other)
        raw_type = act.get("type", "")
        source = Source.github if "Github" in raw_type else Source.velog
        try:
            result.append(RecentActivity(
                source=source,
                category=category,
                tech_stacks=act.get("tech_stacks", [])[:3],
                summary=act.get("summary", ""),
            ))
        except Exception:
            continue
    return result


# ---------------------------------------------------------------------------
# main e2e test
# ---------------------------------------------------------------------------

async def run_e2e_test(use_cached_profile: bool = False) -> tuple[dict, dict]:
    FIXTURES_DIR.mkdir(exist_ok=True)
    RESULTS_DIR.mkdir(exist_ok=True)

    timings: dict[str, float] = {}
    report:  dict = {}

    # ── Step 1: Profile Analysis ─────────────────────────────────────────────
    print("\n" + "=" * 60)
    print("STEP 1 — Profile Analysis")
    print("=" * 60)

    if use_cached_profile and PROFILE_CACHE.exists():
        print("[PROFILE] 캐시 사용 → fixtures/sample_final_profile.json")
        with PROFILE_CACHE.open(encoding="utf-8") as f:
            profile_data = json.load(f)
        timings["profile_total"] = 0.0
    else:
        print("[PROFILE] run_integrated_analysis() 실행 중...")
        profile_data, elapsed = await _measure("profile", run_integrated_analysis())
        timings["profile_total"] = elapsed
        with PROFILE_CACHE.open("w", encoding="utf-8") as f:
            json.dump(profile_data, f, ensure_ascii=False, indent=2)
        print(f"[PROFILE] 완료 {elapsed:.2f}s → 캐시 저장 완료")

    profile    = profile_data["profile"]
    activities = profile_data.get("activities", [])
    skill_freq = profile.get("skill_frequency", {})
    positions  = profile.get("possible_positions", ["backend"])

    print(f"\n  headline          : {profile.get('headline', 'N/A')}")
    print(f"  possible_positions: {positions}")
    print(f"  skill_frequency (top 5): {dict(list(skill_freq.items())[:5])}")

    report["profile"] = profile

    # ── Step 2: LearningState / RecentActivity 파생 ──────────────────────────
    learning_states   = _derive_learning_states(skill_freq)
    recent_activities = _derive_recent_activities(activities)
    weak_skills       = [ls for ls in learning_states if ls.relative_rank == RelativeRank.low]

    print(f"\n  LearningState {len(learning_states)}개 파생 — 약점 {len(weak_skills)}개")
    for ls in learning_states:
        print(f"    {ls.skill:22s}  rank={ls.relative_rank.value:4s}  percentile={ls.percentile}%")

    user = UserContext(
        user_id=1,
        job_role="취준생",
        target_positions=positions[:2],
        survey_context=SurveyContext(
            job_role="취준생",
            target_positions=positions[:2],
            tech_stacks=list(skill_freq.keys())[:5],
            persona="default",
        ),
    )

    # ── Step 3: Recommendation ───────────────────────────────────────────────
    print("\n" + "=" * 60)
    print("STEP 3 — Recommendation")
    print("=" * 60)

    rec_request = RecommendationRequest(
        request_id="e2e_rec_001",
        trace_id="trace_e2e_001",
        user=user,
        learning_states=learning_states,
        recent_activities=recent_activities,
        history_exclusions=[],
    )

    rec_response, rec_elapsed = await _measure(
        "recommendation", recommendation_svc.run_recommendation(rec_request)
    )
    timings["recommendation"] = rec_elapsed
    report["recommendation"]  = rec_response.model_dump()

    print(f"[REC] 완료 {rec_elapsed:.2f}s — {len(rec_response.items)}개 반환")
    for i, item in enumerate(rec_response.items):
        print(f"  [{i+1}] {item.title[:55]}  difficulty={item.difficulty.value}")
        print(f"       → {item.reason[:80]}...")
    print(f"\n  전체 이유: {rec_response.reason[:120]}...")

    # ── Step 4: Quiz (top recommendation 기반) ───────────────────────────────
    print("\n" + "=" * 60)
    print("STEP 4 — Quiz")
    print("=" * 60)

    top_rec = rec_response.items[0]
    top_ref = rec_response.references[0] if rec_response.references else None
    target_skill = (
        top_ref.skill_tags[0]
        if top_ref and top_ref.skill_tags
        else (weak_skills[0].skill if weak_skills else learning_states[0].skill)
    )

    quiz_request = QuizRequest(
        request_id="e2e_quiz_001",
        trace_id="trace_e2e_001",
        user=user,
        target_skill=target_skill,
        current_level=weak_skills[0].relative_rank if weak_skills else RelativeRank.mid,
        quiz_type=QuizType.pre_assessment,
        difficulty=RelativeRank.low,
        time_limit_minutes=15,
        recent_references=[top_ref] if top_ref else [],
        recommendation_hint=RecommendationHint(
            title=top_rec.title,
            reason=top_rec.reason,
            doc_id=top_rec.doc_id,
        ),
    )

    print(f"[QUIZ] target_skill='{target_skill}' 퀴즈 생성 중...")
    quiz_response, quiz_elapsed = await _measure("quiz", quiz_svc.run_quiz(quiz_request))
    timings["quiz"]   = quiz_elapsed
    report["quiz"]    = quiz_response.model_dump()

    print(f"[QUIZ] 완료 {quiz_elapsed:.2f}s — {len(quiz_response.questions)}문항 생성")
    for q in quiz_response.questions:
        label = {"short_answer": "SA", "multiple_choice": "MC", "coding": "CODE"}[q.type.value]
        print(f"  [{q.id}][{label}] {q.question[:75]}...")

    # ── 성능 요약 ─────────────────────────────────────────────────────────────
    print("\n" + "=" * 60)
    print("PERFORMANCE SUMMARY")
    print("=" * 60)

    rows = [
        ("profile_total (캐시=0s)", timings["profile_total"], THRESHOLDS["profile_total"]),
        ("recommendation",           timings["recommendation"], THRESHOLDS["recommendation"]),
        ("quiz",                     timings["quiz"],           THRESHOLDS["quiz"]),
    ]
    hot_total = timings["recommendation"] + timings["quiz"]

    for label, elapsed, threshold in rows:
        if elapsed == 0.0:
            status = "CACHED"
            flag   = ""
        else:
            status = f"{elapsed:.2f}s"
            flag   = " ✓" if elapsed <= threshold else f" ✗ (목표 {threshold}s 초과)"
        print(f"  {label:40s}: {status}{flag}")

    print(f"  {'rec + quiz (hot path)':40s}: {hot_total:.2f}s")

    # ── 결과 저장 ─────────────────────────────────────────────────────────────
    report["timings"]    = timings
    report["thresholds"] = THRESHOLDS
    out_path = RESULTS_DIR / "e2e_result.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2, default=str)
    print(f"\n결과 저장 → {out_path}")

    return report, timings


# ---------------------------------------------------------------------------
# pytest entry point
# ---------------------------------------------------------------------------

def test_e2e_latency():
    """pytest: 전체 파이프라인 latency가 목표치 이내인지 검증."""
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    use_cache = PROFILE_CACHE.exists()
    _, timings = asyncio.run(run_e2e_test(use_cached_profile=use_cache))

    assert timings["recommendation"] <= THRESHOLDS["recommendation"], (
        f"recommendation {timings['recommendation']:.2f}s > 목표 {THRESHOLDS['recommendation']}s"
    )
    assert timings["quiz"] <= THRESHOLDS["quiz"], (
        f"quiz {timings['quiz']:.2f}s > 목표 {THRESHOLDS['quiz']}s"
    )
    if timings["profile_total"] > 0:
        assert timings["profile_total"] <= THRESHOLDS["profile_total"], (
            f"profile {timings['profile_total']:.2f}s > 목표 {THRESHOLDS['profile_total']}s"
        )


# ---------------------------------------------------------------------------
# standalone entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    use_cache = "--cache" in sys.argv
    asyncio.run(run_e2e_test(use_cached_profile=use_cache))

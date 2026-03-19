"""
퀴즈 품질 메트릭 테스트

측정 항목:
    1. type_distribution  — 실제 문항 유형이 스펙 테이블과 일치하는지
    2. rubric_coverage    — full_score_criteria 개수, SA 문항에 expected_points 충분한지
    3. groundedness       — SA/coding 문항에 scoring_keywords(Qdrant 추출) 포함 여부
    4. personalization    — recommendation_hint 있을 때 질문 본문에 스킬명 포함 비율
    5. mc_correctness     — MC 문항에 options 4개, correct 인덱스 유효 여부

테스트 매트릭스:
    pre_assessment × {low, mid}
    review         × {low, high}
    interview      × {mid, high}

실행 방법:
    python -m pytest tests/test_quiz_quality.py -s -v
    python tests/test_quiz_quality.py
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
    FreshnessGrade,
    QuestionType,
    QuizRequest,
    QuizResponse,
    QuizType,
    RecommendationHint,
    ReferenceItem,
    RelativeRank,
    SurveyContext,
    UserContext,
)
from app.services import quiz_svc
from app.services.quiz_svc import _QUESTION_COUNT, _QUESTION_TYPE_MIX, _select_question_types

RESULTS_DIR = Path(__file__).parent / "results"

TEST_MATRIX = [
    (QuizType.pre_assessment, RelativeRank.low),
    (QuizType.pre_assessment, RelativeRank.mid),
    (QuizType.review,         RelativeRank.low),
    (QuizType.review,         RelativeRank.high),
    (QuizType.interview,      RelativeRank.mid),
    (QuizType.interview,      RelativeRank.high),
]


# ---------------------------------------------------------------------------
# 요청 빌더
# ---------------------------------------------------------------------------

def _user() -> UserContext:
    return UserContext(
        user_id=99,
        job_role="취준생",
        target_positions=["backend"],
        survey_context=SurveyContext(
            job_role="취준생",
            target_positions=["backend"],
            tech_stacks=["Spring Boot", "JWT"],
            persona="default",
        ),
    )


def _make_quiz_request(
    quiz_type: QuizType,
    difficulty: RelativeRank,
    target_skill: str = "JWT",
    with_hint: bool = True,
) -> QuizRequest:
    hint = RecommendationHint(
        title="JWT Best Current Practices (RFC 8725)",
        reason="JWT 역량이 현재 낮은 수준이므로 기초부터 다지는 것을 추천합니다.",
        doc_id="ref_jwt_001",
    ) if with_hint else None

    return QuizRequest(
        request_id=f"test_{quiz_type.value}_{difficulty.value}",
        trace_id="trace_quiz_quality",
        user=_user(),
        target_skill=target_skill,
        current_level=difficulty,
        quiz_type=quiz_type,
        difficulty=difficulty,
        time_limit_minutes=20,
        recent_references=[
            ReferenceItem(
                doc_id="ref_jwt_001",
                title="JWT Best Current Practices (RFC 8725)",
                url="https://datatracker.ietf.org/doc/html/rfc8725",
                freshness=FreshnessGrade.stable,
                skill_tags=["JWT", "Security"],
                source_type="official_doc",
            )
        ],
        recommendation_hint=hint,
    )


# ---------------------------------------------------------------------------
# 품질 메트릭 함수
# ---------------------------------------------------------------------------

def measure_type_distribution(response: QuizResponse, request: QuizRequest) -> dict:
    """
    실제 문항 유형 순서가 스펙 테이블과 일치하는지.
    count > 기본 패턴 길이인 경우 _select_question_types로 순환 확장한 결과와 비교.
    기준: count_match AND type_seq_match 모두 True
    """
    expected_count = _QUESTION_COUNT[request.quiz_type][request.difficulty]
    # 서비스와 동일한 확장 로직 사용 (순환 포함)
    expected_expanded = _select_question_types(request.quiz_type, request.difficulty, expected_count)
    actual_types      = [q.type for q in response.questions]

    count_match    = len(response.questions) == expected_count
    type_seq_match = actual_types == expected_expanded

    return {
        "expected_count":   expected_count,
        "actual_count":     len(response.questions),
        "expected_types":   [t.value for t in expected_expanded],
        "actual_types":     [t.value for t in actual_types],
        "count_match":      count_match,
        "type_seq_match":   type_seq_match,
        "pass":             count_match and type_seq_match,
    }


def measure_rubric_coverage(response: QuizResponse, request: QuizRequest) -> dict:
    """
    루브릭 충실도:
    - full_score_criteria ≥ 2개
    - SA/coding 문항의 expected_points ≥ 2개인 비율 ≥ 50%
    """
    skill = request.target_skill.lower()

    full_count      = len(response.rubric.full_score_criteria)
    skill_in_rubric = any(skill in c.lower() for c in response.rubric.full_score_criteria)

    sa_questions = [q for q in response.questions if q.type != QuestionType.multiple_choice]
    enough_points = sum(1 for q in sa_questions if len(q.expected_points) >= 2)
    point_rate    = enough_points / len(sa_questions) if sa_questions else 1.0

    return {
        "full_criteria_count":   full_count,
        "skill_in_rubric":       skill_in_rubric,
        "sa_questions":          len(sa_questions),
        "enough_points_count":   enough_points,
        "sa_point_coverage":     round(point_rate, 3),
        "pass":                  full_count >= 2 and point_rate >= 0.5,
    }


def measure_groundedness(response: QuizResponse, request: QuizRequest) -> dict:
    """
    SA/coding 문항의 expected_points 마지막 줄에 '필수 키워드:' 항목이 있는지.
    Qdrant 청크에서 추출된 키워드가 있을 때만 추가됨 (anti-hallucination 지표).
    기준: SA/coding 문항 중 ≥ 50%가 scoring_keywords 보유
    """
    sa_or_coding = [q for q in response.questions if q.type != QuestionType.multiple_choice]
    if not sa_or_coding:
        return {"pass": True, "note": "SA/coding 문항 없음"}

    with_keywords = sum(
        1 for q in sa_or_coding
        if any("필수 키워드:" in ep for ep in q.expected_points)
    )
    rate = with_keywords / len(sa_or_coding)

    return {
        "sa_coding_count":      len(sa_or_coding),
        "with_keywords_count":  with_keywords,
        "keyword_rate":         round(rate, 3),
        "pass":                 rate >= 0.5,
    }


def measure_personalization(response: QuizResponse, request: QuizRequest) -> dict:
    """
    recommendation_hint 있을 때, 질문 본문에 target_skill 언급 비율.
    기준: ≥ 50%
    """
    if request.recommendation_hint is None:
        return {"pass": True, "note": "hint 없음"}

    skill       = request.target_skill.lower()
    personalized = sum(1 for q in response.questions if skill in q.question.lower())
    rate         = personalized / len(response.questions) if response.questions else 0.0

    return {
        "personalized_count": personalized,
        "total_questions":    len(response.questions),
        "rate":               round(rate, 3),
        "pass":               rate >= 0.5,
    }


def measure_mc_correctness(response: QuizResponse) -> dict:
    """
    MC 문항 형식 검증: options 4개, correct 인덱스 0~3 유효.
    기준: 모든 MC 문항 통과
    """
    mc_questions = [q for q in response.questions if q.type == QuestionType.multiple_choice]
    if not mc_questions:
        return {"pass": True, "note": "MC 문항 없음"}

    valid = sum(
        1 for q in mc_questions
        if q.options and len(q.options) == 4
        and q.correct is not None and 0 <= q.correct <= 3
    )
    return {
        "mc_count": len(mc_questions),
        "valid_mc": valid,
        "pass":     valid == len(mc_questions),
    }


# ---------------------------------------------------------------------------
# 테스트 실행기
# ---------------------------------------------------------------------------

async def run_quiz_quality_tests() -> dict:
    RESULTS_DIR.mkdir(exist_ok=True)
    all_results: dict = {}

    for quiz_type, difficulty in TEST_MATRIX:
        case_name = f"{quiz_type.value}/{difficulty.value}"
        print(f"\n{'=' * 60}")
        print(f"케이스: {case_name}")
        print("=" * 60)

        request  = _make_quiz_request(quiz_type, difficulty)
        response = await quiz_svc.run_quiz(request)

        label_map = {"short_answer": "SA", "multiple_choice": "MC", "coding": "CODE"}
        print(f"{len(response.questions)}문항 생성:")
        for q in response.questions:
            print(f"  [{q.id}][{label_map[q.type.value]}] {q.question[:70]}...")

        metrics = {
            "type_distribution": measure_type_distribution(response, request),
            "rubric_coverage":   measure_rubric_coverage(response, request),
            "groundedness":      measure_groundedness(response, request),
            "personalization":   measure_personalization(response, request),
            "mc_correctness":    measure_mc_correctness(response),
        }

        print("\n품질 메트릭:")
        all_passed = True
        for name, result in metrics.items():
            flag = "PASS" if result["pass"] else "FAIL"
            if not result["pass"]:
                all_passed = False
            print(f"  [{flag}] {name:22s} {result}")

        print(f"\n종합: {'ALL PASS' if all_passed else 'HAS FAILURES'}")

        all_results[case_name] = {
            "questions":  [q.model_dump() for q in response.questions],
            "rubric":     response.rubric.model_dump(),
            "metrics":    metrics,
            "all_passed": all_passed,
        }

    # 집계 요약
    print("\n" + "=" * 60)
    print("AGGREGATE SUMMARY")
    print("=" * 60)
    passed = sum(1 for v in all_results.values() if v["all_passed"])
    print(f"케이스 통과: {passed}/{len(TEST_MATRIX)}")
    for case_name, result in all_results.items():
        flag   = "PASS" if result["all_passed"] else "FAIL"
        failed = [m for m, r in result["metrics"].items() if not r["pass"]]
        suffix = f"  ← FAILED: {', '.join(failed)}" if failed else ""
        print(f"  [{flag}] {case_name}{suffix}")

    out_path = RESULTS_DIR / "quiz_quality.json"
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(all_results, f, ensure_ascii=False, indent=2, default=str)
    print(f"\n결과 저장 → {out_path}")
    return all_results


# ---------------------------------------------------------------------------
# pytest entry points
# ---------------------------------------------------------------------------

def test_quiz_type_distribution():
    """pytest: 모든 케이스에서 문항 유형이 스펙 테이블과 일치해야 함."""
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    async def _run():
        failures = []
        for quiz_type, difficulty in TEST_MATRIX:
            request  = _make_quiz_request(quiz_type, difficulty)
            response = await quiz_svc.run_quiz(request)
            result   = measure_type_distribution(response, request)
            if not result["pass"]:
                failures.append(f"{quiz_type.value}/{difficulty.value}: {result}")
        return failures

    failures = asyncio.run(_run())
    assert not failures, "type_distribution FAIL:\n" + "\n".join(failures)


def test_quiz_mc_correctness():
    """pytest: MC 문항은 반드시 options 4개 + valid correct 인덱스를 가져야 함."""
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    async def _run():
        failures = []
        for quiz_type, difficulty in TEST_MATRIX:
            request  = _make_quiz_request(quiz_type, difficulty)
            response = await quiz_svc.run_quiz(request)
            result   = measure_mc_correctness(response)
            if not result["pass"]:
                failures.append(f"{quiz_type.value}/{difficulty.value}: {result}")
        return failures

    failures = asyncio.run(_run())
    assert not failures, "mc_correctness FAIL:\n" + "\n".join(failures)


# ---------------------------------------------------------------------------
# standalone entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(run_quiz_quality_tests())

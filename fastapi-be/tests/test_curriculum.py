"""
커리큘럼 생성 서비스 테스트

실행 방법:
    python tests/test_curriculum.py
"""

import asyncio
import json
import os
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

os.environ.setdefault("OPENAI_API_KEY",  "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246")
os.environ.setdefault("OPENAI_API_BASE", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")

from app.models.schemas import (
    CurriculumRequest,
    GoogleCalendarEvent,
    ManualGeneration,
)
from app.services.curriculum_svc import run_curriculum

RESULTS_DIR = Path(__file__).parent / "results"
RESULTS_DIR.mkdir(exist_ok=True)


def _make_request() -> CurriculumRequest:
    return CurriculumRequest(
        userId=1,
        considerPersonalSchedule=True,
        googleCalendarEvents=[
            GoogleCalendarEvent(
                title="중간고사",
                startDate="2026-04-20",
                endDate="2026-04-25",
            )
        ],
        profileData={
            "level": "중급",
            "techStacks": ["Java", "Spring", "Docker"],
            "desiredPositions": ["백엔드 개발자"],
        },
        manualGeneration=ManualGeneration(
            isManual=True,
            topic="Spring Security와 JWT",
            goalType="개념 이해 및 실습",
            specificGoal="실무에 적용할 수 있는 수준",
            surveyAnswers={
                "q1Concept": "기본 개념은 알고 있음",
                "q2Experience": "개인 프로젝트에서 사용해봤음",
                "q3Reason": "실무 투입 전 보강 필요",
            },
        ),
    )


async def main():
    request = _make_request()

    print("=" * 60)
    print("커리큘럼 생성 테스트")
    print("=" * 60)
    print(f"  user_id              : {request.user_id}")
    print(f"  topic                : {request.manual_generation.topic}")
    print(f"  considerSchedule     : {request.consider_personal_schedule}")
    print(f"  googleCalendarEvents : {len(request.google_calendar_events)}개")
    print()

    start = time.perf_counter()
    response = await run_curriculum(request)
    elapsed = time.perf_counter() - start

    print(f"[완료] {elapsed:.2f}s  {len(response.nodes)}개 노드 생성")
    print()
    print("=== recommendationReason ===")
    print(f"  summaryLine       : {response.recommendation_reason.summary_line}")
    print(f"  userContext       : {response.recommendation_reason.user_context}")
    print(f"  aiInterpretation  : {response.recommendation_reason.ai_interpretation}")
    print(f"  curriculumRationale: {response.recommendation_reason.curriculum_rationale}")
    print()
    print("=== nodes ===")
    for node in response.nodes:
        print(f"  [{node.scheduled_date}] {node.title} ({node.expected_minutes}분)")
        if node.description:
            print(f"         {node.description}")

    result = response.model_dump(by_alias=True)
    result["elapsed"] = elapsed

    out_path = RESULTS_DIR / "curriculum_result.json"
    out_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print()
    print(f"결과 저장 → {out_path}")


if __name__ == "__main__":
    asyncio.run(main())

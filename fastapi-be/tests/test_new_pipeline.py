import asyncio
import os
import sys

# 경로 등록
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.models.schemas import (
    ActivityHistoryItem,
    AnalysisData,
    CurriculumRequest,
    CurriculumType,
    DailyRecommendationRequest,
    SkillStat,
    TechDetail,
    UserLevel,
)
from app.services.curriculum_svc import run_curriculum
from app.services.profile_analyzer import analyze_from_activity_history
from app.services.recommendation_svc import run_daily_recommendation

async def test_analyze_from_activity_history():
    print("\n" + "="*50)
    print("1. [테스트] Activity History 기반 프로필 분석")
    print("="*50)
    
    records = [
        ActivityHistoryItem(
            activity_type="COMMIT",
            category="개발",
            title="Spring Security JWT 필터 구현",
            activity_date="2026-03-20T10:00:00",
            tech_stacks=["Spring Boot", "Spring Security", "JWT"]
        ),
        ActivityHistoryItem(
            activity_type="VELOG",
            category="학습",
            title="FastAPI 비동기 처리 이해하기",
            activity_date="2026-03-21T14:30:00",
            tech_stacks=["FastAPI", "Python", "asyncio"]
        )
    ]
    
    result = await analyze_from_activity_history(records)
    print(result.get("profile", {}))


async def test_curriculum_onboarding():
    print("\n" + "="*50)
    print("2. [테스트] ONBOARDING 커리큘럼 생성 (profile_analyzer 연동)")
    print("="*50)
    
    req = CurriculumRequest(
        user_id=1,
        curriculum_type=CurriculumType.onboarding,
        analysis_data=AnalysisData(
            summary="Spring Boot 기반 백엔드 단기 집중 역량 확보 중",
            tech_details=[TechDetail(tech_name="Spring Boot", proficiency_percentage=80, usage_count=10)],
            recommended_positions=[]
        ),
        user_tech_stacks=[SkillStat(skill="Spring Boot", count=10)]
    )
    
    res = await run_curriculum(req)
    print("Summary:", res.recommendation_reason.summary_line)
    for n in res.nodes:
        print(f" - [{n.scheduled_date}] {n.title} ({n.expected_minutes}분)")


async def test_daily_recommendation():
    print("\n" + "="*50)
    print("3. [테스트] Daily Recommendation (자정 배치 추천)")
    print("="*50)
    
    req = DailyRecommendationRequest(
        user_id=1,
        curriculum_id=10,
        current_level=UserLevel.mid,
        favorite_tech_stacks=["Python", "FastAPI"],
        skill_stats=[SkillStat(skill="FastAPI", count=5)],
        recent_activities=[
            ActivityHistoryItem(
                activity_type="COMMIT",
                category="개발",
                title="FastAPI 라우터 구조 개편",
                activity_date="2026-03-22T09:00:00",
                tech_stacks=["FastAPI", "Python"]
            )
        ]
    )
    
    res = await run_daily_recommendation(req)
    print("Reason Summary:", res.recommendation_reason.summary)
    print("Status Summary:", res.current_status.summary)
    print("Top Skills:", res.current_status.top_skills)
    print("Next Nodes:")
    for n in res.next_nodes:
        print(f" - {n.title}")

async def main():
    await test_analyze_from_activity_history()
    await test_curriculum_onboarding()
    await test_daily_recommendation()

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(main())

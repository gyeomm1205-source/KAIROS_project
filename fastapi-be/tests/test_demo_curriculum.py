import asyncio
import sys
import os
import time
from dotenv import load_dotenv

# dotenv 명시적 로딩 (LLM API Key)
load_dotenv()

# PATH 설정 (app 모듈 참조용)
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from app.models.schemas import (
    AnalysisData,
    GoogleCalendarEvent,
    CurriculumRequest,
    CurriculumType,
    RecommendedPosition,
    SkillStat,
    TechDetail,
)
from app.services.curriculum_svc import run_curriculum

async def run_demo():
    print("="*60)
    print("🚀 [DEMO] 최초 가입 직후 ONBOARDING 커리큘럼 생성")
    print("="*60)

    # 1. 직전 단계(Profile Analyzer)에서 생성된 실제 zhyon 계정 기반 1페이지 프로필 데이터
    analysis_data = AnalysisData(
        summary="다양한 기술 스택을 활용한 웹 및 서버 개발 전문가",
        tech_details=[
            TechDetail(tech_name="Java", proficiency_percentage=80, usage_count=38),
            TechDetail(tech_name="JavaScript", proficiency_percentage=75, usage_count=36),
            TechDetail(tech_name="C++", proficiency_percentage=70, usage_count=34),
            TechDetail(tech_name="React", proficiency_percentage=60, usage_count=20),
        ],
        recommended_positions=[
            RecommendedPosition(position_name="핀테크 플랫폼의 실시간 데이터 처리 및 사용자 인터페이스 개발 엔지니어", fit_level="HIGH"),
            RecommendedPosition(position_name="AI 기반의 웹 애플리케이션 백엔드 아키텍처 설계 및 구현 전문가", fit_level="HIGH")
        ]
    )

    # 2. 프로필의 빈도수 기반 스택 통계
    user_tech_stacks = [
        SkillStat(skill="Java", count=38),
        SkillStat(skill="JavaScript", count=36),
        SkillStat(skill="C++", count=34),
        SkillStat(skill="React", count=20)
    ]

    # 3. 구글 캘린더 연동 데이터 (가상의 일정 세팅, YYYY-MM-DD)
    calendar_events = [
        GoogleCalendarEvent(title="아침 전체 스크럼", start_date="2026-03-24", end_date="2026-03-24"),
        GoogleCalendarEvent(title="프로젝트 기획 회의", start_date="2026-03-24", end_date="2026-03-24"),
        GoogleCalendarEvent(title="SSAFY 알고리즘 코딩 스터디", start_date="2026-03-25", end_date="2026-03-25"),
        GoogleCalendarEvent(title="팀 점심 회식", start_date="2026-03-26", end_date="2026-03-26")
    ]

    req = CurriculumRequest(
        user_id=1,
        curriculum_type=CurriculumType.onboarding,
        analysis_data=analysis_data,
        user_tech_stacks=user_tech_stacks,
        google_calendar_events=calendar_events
    )

    print("\n⏳ zhyon 님의 프로필과 구글 캘린더를 분석하여 커리큘럼을 생성 중입니다... (LLM 처리 중)\n")
    try:
        start_time = time.time()
        response = await run_curriculum(req)
        end_time = time.time()
        
        print("✅ 커리큘럼 생성 완료!\n")
        print(f"⏱️ [생성 소요 시간]: {end_time - start_time:.2f}초\n")
        print(f"🎯 [커리큘럼 한줄 요약]\n{response.recommendation_reason.summary_line}\n")
        print(f"💡 [AI 해석 (왜 이렇게 짰는지)]\n{response.recommendation_reason.ai_interpretation}\n")
        
        print("-" * 60)
        print("📅 [상세 목차 (5일치 추천 일정)]")
        for idx, node in enumerate(response.nodes):
            print(f"Day {idx+1}. [{node.scheduled_date[:10]}] {node.title} ({node.expected_minutes}분)")
            print(f"    - 설명: {node.description}")
        print("-" * 60)

    except Exception as e:
        print(f"❌ 에러 발생: {e}")

if __name__ == "__main__":
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(run_demo())

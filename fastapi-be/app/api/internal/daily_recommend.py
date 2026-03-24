"""
자정 배치 커리큘럼 기반 추천 및 성장 요약 생성 API 라우터.
"""

from fastapi import APIRouter

from app.models.schemas import (
    DailyRecommendationPayload,
    DailyRecommendationRequest,
    GrowthSummaryRequest,
    GrowthSummaryResponse,
)
from app.services.growth_svc import generate_growth_summary
from app.services.recommendation_svc import run_daily_recommendation

router = APIRouter(prefix="/api/v1/ai", tags=["internal-ai"])


@router.post(
    "/recommendations/daily-generate",
    response_model=DailyRecommendationPayload,
    summary="[Internal] 자정 배치 / 온보딩 직후 추천 Payload 생성",
    description="""
    SpringBoot에서 전달한 skill_stats과 recent_activities를 바탕으로 Qdrant를 검색하고,
    LLM을 호출해 추천이유 + 현상태 + 다음노드를 한 번에 생성합니다.
    (반환된 DailyRecommendationPayload는 Redis recommendation:{userId}:{curriculumId} 에 저장됩니다.)
    """
)
async def generate_daily_recommendation(request: DailyRecommendationRequest):
    return await run_daily_recommendation(request)


@router.post(
    "/growth/summary",
    response_model=GrowthSummaryResponse,
    summary="[Client] 성장 일지 요약 코멘트 생성",
    description="""
    SpringBoot의 GET /activities/growth-report 통계를 기반으로,
    LLM이 사용자에게 제공할 단 한 문장의 격려/상태 요약 메시지를 생성합니다.
    """
)
async def create_growth_summary(request: GrowthSummaryRequest):
    return await generate_growth_summary(request)

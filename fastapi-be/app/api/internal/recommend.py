"""
Recommendation endpoint — POST /api/v1/ai/recommendations/generate
Issue: S14P21A506-146

Called by Spring Boot batch (매일 자정) or on-demand.
Returns recommendationReason, nextNodes, references to be cached in Redis.
"""

import logging

from fastapi import APIRouter

from app.models.schemas import RecommendationRequest, RecommendationResponse
from app.services.recommendation_svc import run_recommendation

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai", tags=["AI Services"])


@router.post(
    "/recommendations/generate",
    response_model=RecommendationResponse,
    response_model_by_alias=True,
)
async def create_recommendation(request: RecommendationRequest) -> RecommendationResponse:
    """
    커리큘럼별 맞춤 추천 데이터를 생성한다.

    - favoriteTechStacks 기반으로 Qdrant에서 레퍼런스를 검색한다.
    - currentLevel에 따라 난이도 적합 자료를 우선 반환한다.
    - Spring Boot가 반환값을 Redis(recommendation:{userId}:{curriculumId})에 캐싱한다.
    """
    logger.info(
        "POST /api/v1/ai/recommendations/generate  user_id=%s  level=%s",
        request.user_id,
        request.current_level.value,
    )
    response = await run_recommendation(request)
    logger.info(
        "POST /api/v1/ai/recommendations/generate  DONE  user_id=%s  refs=%d",
        request.user_id,
        len(response.references),
    )
    return response

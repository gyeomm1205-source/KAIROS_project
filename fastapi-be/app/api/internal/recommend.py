"""
Internal router — POST /internal/recommendations

Called exclusively by Spring Boot. Never exposed to the public internet.
FastAPI receives a fully assembled AIContext (RecommendationRequest) and
returns a RecommendationResponse. No MySQL access happens here.

Spring Boot ↔ FastAPI contract (AI Tech Spec v3, §9-3):
  Request:  RecommendationRequest  (learning_states, recent_activities, history_exclusions, ...)
  Response: RecommendationResponse (items, reason, assumptions, references, model_meta)
"""

import logging

from fastapi import APIRouter, HTTPException, status
from fastapi.responses import JSONResponse

from app.models.schemas import MinimalFallbackResponse, RecommendationRequest, RecommendationResponse
from app.services.recommendation_svc import run_recommendation

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/internal", tags=["Internal AI Services"])


@router.post(
    "/recommendations",
    response_model=RecommendationResponse,
    summary="Generate personalized learning recommendations",
    description=(
        "Receives a fully assembled AIContext from Spring Boot and returns "
        "a ranked list of learning references with recommendation reasons. "
        "**Not for public use** — service-to-service only."
    ),
    responses={
        200: {"description": "Recommendations generated successfully"},
        422: {"description": "Invalid AIContext payload"},
        500: {
            "model": MinimalFallbackResponse,
            "description": "LLM / retrieval failure — minimal fallback response returned",
        },
    },
)
async def create_recommendation(
    request: RecommendationRequest,
) -> RecommendationResponse:
    """
    POST /internal/recommendations

    Orchestration (handled by recommendation_svc.run_recommendation):
      1. Retrieve candidates  — mock Qdrant results for now
      2. Filter exclusions    — remove already-seen doc_ids
      3. Filter by skill      — keep skill-relevant docs
      4. Rank by priority     — weakest skills first
      5. Select top-K
      6. Generate reason      — LLM (gpt-4o) with template fallback
      7. Assemble response    — RecommendationResponse
    """
    logger.info(
        "POST /internal/recommendations  request_id=%s  user_id=%s  skills=%d",
        request.request_id,
        request.user.user_id,
        len(request.learning_states),
    )

    try:
        response = await run_recommendation(request)
        logger.info(
            "POST /internal/recommendations  DONE  request_id=%s  items=%d",
            request.request_id,
            len(response.items),
        )
        return response

    except Exception as exc:
        # Spec §14: LLM 구조화 실패 시 최소 스키마 보장
        logger.exception(
            "POST /internal/recommendations  FAILED  request_id=%s  error=%s",
            request.request_id,
            exc,
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={
                "error": str(exc),
                "fallback": True,
                "references": [],
                "assumptions": [],
                "warnings": ["추천 생성 중 오류가 발생했습니다. Spring Boot에서 재시도하거나 fallback 응답을 사용하세요."],
            },
        )

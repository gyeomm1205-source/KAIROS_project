"""
Profile feedback endpoint — POST /api/v1/ai/profile/feedback
Issue: S14P21A506-210

Frontend가 직접 호출. 이전 분석 결과와 피드백을 받아 국소 수정 후 반환.
"""

import logging

from fastapi import APIRouter

from app.models.schemas import ProfileFeedbackRequest, ProfileFeedbackResponse
from app.services.profile_analyzer import regenerate_profile_with_feedback

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/ai", tags=["AI Services"])


@router.post(
    "/profile/feedback",
    response_model=ProfileFeedbackResponse,
    response_model_by_alias=True,
)
async def profile_feedback(request: ProfileFeedbackRequest) -> ProfileFeedbackResponse:
    """
    이전 분석 결과에 사용자 피드백을 반영하여 국소 수정 후 반환한다.

    - 전체 재분석 없이 기존 분석문 + 피드백만으로 수정
    - 응답 구조는 SSE COMPLETED event의 data 구조와 동일
    - Frontend는 응답을 브라우저 Cache에 덮어씌워 저장
    """
    logger.info(
        "POST /api/v1/ai/profile/feedback  user_id=%s",
        request.user_id,
    )

    previous = request.previous_analysis.model_dump(by_alias=False)
    result = await regenerate_profile_with_feedback(previous, request.user_feedback)

    # LLM 반환값(skill_frequency, recommended_positions)을 명세서 형식으로 변환
    skill_freq = result.get("skill_frequency", {})
    tech_details = [
        {"techName": name, "proficiencyPercentage": 0, "usageCount": count}
        for name, count in skill_freq.items()
    ]

    raw_positions = result.get("recommended_positions", {})
    recommended_positions = [
        {"positionName": name, "fitLevel": desc}
        for name, desc in raw_positions.items()
    ]

    return ProfileFeedbackResponse(
        summary=result.get("summary", ""),
        tech_details=tech_details,
        recommended_positions=recommended_positions,
    )

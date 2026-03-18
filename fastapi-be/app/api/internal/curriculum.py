"""
Curriculum generation endpoint — POST /api/v1/ai/curriculum/generate
Issue: S14P21A506-147

Receives a CurriculumRequest from Spring Boot (or frontend) and
delegates to curriculum_svc.run_curriculum().
"""

from fastapi import APIRouter

from app.models.schemas import CurriculumRequest, CurriculumResponse
from app.services.curriculum_svc import run_curriculum

router = APIRouter(prefix="/api/v1/ai", tags=["AI Services"])


@router.post(
    "/curriculum/generate",
    response_model=CurriculumResponse,
    response_model_by_alias=True,
)
async def create_curriculum(request: CurriculumRequest) -> CurriculumResponse:
    """
    프로필과 일정 제약을 바탕으로 날짜가 지정된 맞춤형 커리큘럼을 생성한다.

    - profileData에서 사용자 수준/기술 스택/희망 포지션을 추출한다.
    - manualGeneration이 있으면 수동 주제 기반으로 생성한다.
    - considerPersonalSchedule=true이면 googleCalendarEvents를 참고해 일정을 배치한다.
    """
    return await run_curriculum(request)

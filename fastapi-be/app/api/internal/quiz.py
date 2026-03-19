"""
Quiz generation endpoint — POST /api/v1/ai/quizzes/generate-async
Issue: S14P21A506-121

Receives a QuizRequest from Spring Boot and delegates to quiz_svc.run_quiz().
Spring Boot stores the response (with correctAnswer) in Redis quiz_preview.
"""

from fastapi import APIRouter

from app.models.schemas import QuizRequest, QuizResponse
from app.services.quiz_svc import run_quiz

router = APIRouter(prefix="/api/v1/ai", tags=["AI Services"])


@router.post(
    "/quizzes/generate-async",
    response_model=QuizResponse,
    response_model_by_alias=True,
)
async def create_quiz(request: QuizRequest) -> QuizResponse:
    """
    커리큘럼 기반 퀴즈를 사전 생성해 반환한다.

    - targetTechStacks에서 첫 번째 기술을 주요 출제 범위로 사용한다.
    - userLevel에 따라 문항 난이도와 유형을 결정한다.
    - correctAnswer는 Spring Boot가 Redis에만 저장하고 클라이언트에는 노출하지 않는다.
    """
    return await run_quiz(request)

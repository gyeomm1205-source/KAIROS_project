"""
Quiz generation endpoint — POST /internal/quizzes
Issue: S14P21A506-121

Receives a QuizRequest from Spring Boot and delegates to quiz_svc.run_quiz().
Supports RecommendationHint context when triggered from the recommendation screen.
"""

from fastapi import APIRouter

from app.models.schemas import QuizRequest, QuizResponse
from app.services.quiz_svc import run_quiz

router = APIRouter(prefix="/internal", tags=["Internal AI Services"])


@router.post("/quizzes", response_model=QuizResponse)
async def create_quiz(request: QuizRequest) -> QuizResponse:
    """
    Generate a personalized quiz for the given skill and user context.

    - Retrieves relevant content chunks from Qdrant as RAG grounding.
    - Generates questions via gpt-4o with structured output.
    - Personalizes questions using RecommendationHint when provided
      (triggered from the recommendation screen via S14P21A506-121).
    - Returns a structured QuizResponse: questions + rubric + model_meta.
    """
    return await run_quiz(request)

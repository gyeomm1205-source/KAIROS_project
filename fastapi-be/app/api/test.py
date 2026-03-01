from datetime import datetime, timezone

from fastapi import APIRouter

from app.schemas.common import ErrorResponse
from app.schemas.ping import PingResponse
from app.services.spring_client import SpringClient

router = APIRouter(prefix="/api/test", tags=["Test Ping API"])


@router.get(
    "/ping",
    response_model=PingResponse,
    summary="FastAPI ping",
)
async def ping() -> PingResponse:
    return PingResponse(
        source="fastapi-be",
        message="pong",
        timestamp=datetime.now(timezone.utc),
        downstream=None,
    )


@router.get(
    "/ping/spring",
    response_model=PingResponse,
    summary="FastAPI to Spring ping",
    responses={
        502: {
            "model": ErrorResponse,
            "description": "Spring Boot unavailable",
        }
    },
)
async def ping_spring() -> PingResponse:
    spring_response = await SpringClient().ping()
    return PingResponse(
        source="fastapi-be",
        message="fastapi-to-spring-ok",
        timestamp=datetime.now(timezone.utc),
        downstream=spring_response,
    )

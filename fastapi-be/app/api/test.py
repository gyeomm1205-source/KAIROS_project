from datetime import datetime, timezone

from fastapi import APIRouter, Depends

from app.schemas.auth import CurrentUserResponse
from app.schemas.common import ErrorResponse
from app.schemas.ping import PingResponse
from app.services.auth import CognitoUser, get_current_user
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


@router.get(
    "/me",
    response_model=CurrentUserResponse,
    summary="FastAPI current user",
    responses={
        401: {
            "model": ErrorResponse,
            "description": "Unauthorized",
        }
    },
)
async def current_user(current_user: CognitoUser = Depends(get_current_user)) -> CurrentUserResponse:
    return CurrentUserResponse(
        source="fastapi-be",
        subject=current_user.subject,
        username=current_user.username,
        email=current_user.email,
        groups=current_user.groups,
        token_use=current_user.token_use,
        client_id=current_user.client_id,
    )

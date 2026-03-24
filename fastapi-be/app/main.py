import os
from datetime import datetime, timezone

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette import status

from app.api.test import router as test_router
from app.schemas.common import ErrorResponse
from app.services.errors import AppError


def resolve_cors_origins() -> list[str]:
    raw = os.getenv(
        "FASTAPI_CORS_ALLOWED_ORIGINS",
        "http://localhost:5173,http://127.0.0.1:5173,http://localhost:5174,http://127.0.0.1:5174,https://kairos.cloud-ip.cc,http://kairos.cloud-ip.cc",
    )
    return [origin.strip() for origin in raw.split(",") if origin.strip()]


app = FastAPI(
    title="S14P21A506 FastAPI",
    version="0.0.1",
    description="FastAPI test APIs for Spring ping-pong integration.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=resolve_cors_origins(),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(test_router)


@app.exception_handler(AppError)
async def handle_app_error(request: Request, exc: AppError) -> JSONResponse:
    body = ErrorResponse(
        code=exc.code,
        message=exc.message,
        timestamp=datetime.now(timezone.utc),
        path=request.url.path,
    )
    return JSONResponse(status_code=exc.status_code, content=body.model_dump(mode="json"))


@app.exception_handler(RequestValidationError)
async def handle_validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
    body = ErrorResponse(
        code="COMMON-002",
        message="Invalid request value.",
        timestamp=datetime.now(timezone.utc),
        path=request.url.path,
    )
    return JSONResponse(status_code=status.HTTP_400_BAD_REQUEST, content=body.model_dump(mode="json"))


@app.exception_handler(Exception)
async def handle_internal_error(request: Request, exc: Exception) -> JSONResponse:
    body = ErrorResponse(
        code="COMMON-001",
        message="Internal server error.",
        timestamp=datetime.now(timezone.utc),
        path=request.url.path,
    )
    return JSONResponse(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, content=body.model_dump(mode="json"))

from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, Request, BackgroundTasks, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette import status
import uuid

from app.api.internal.ingestion import router as internal_ingestion_router
from app.api.internal.quiz import router as internal_quiz_router
from app.api.internal.recommend import router as internal_recommend_router
from app.api.test import router as test_router
from app.schemas.common import ErrorResponse
from app.services.errors import AppError
from app.services.qdrant_client import close_qdrant_client, get_qdrant_client, init_collections
from app.services.profile_analyzer import run_integrated_analysis


@asynccontextmanager
async def lifespan(_: FastAPI):
    get_qdrant_client()
    init_collections()
    try:
        yield
    finally:
        close_qdrant_client()


app = FastAPI(
    title="S14P21A506 FastAPI",
    version="0.0.1",
    description="Internal AI bridge for Spring Boot and Qdrant.",
    lifespan=lifespan,
)

app.include_router(test_router)
app.include_router(internal_quiz_router)
app.include_router(internal_recommend_router)
app.include_router(internal_ingestion_router)

# 데모용 인메모리 저장소 (실제로는 DB로 교체 필요)
fake_db = {}


async def background_sync_worker(task_id: str, github_id: str):
    """실제 분석이 일어나는 백그라운드 함수"""
    try:
        fake_db[task_id] = {"status": "processing", "data": None}
        results = await run_integrated_analysis()
        fake_db[task_id]["status"] = "completed"
        fake_db[task_id]["data"] = {
            "profile": results["profile"],
            "activities": results["activities"]
        }
    except Exception as e:
        fake_db[task_id]["status"] = "failed"
        fake_db[task_id]["error_message"] = str(e)


@app.post("/api/profile/sync", status_code=202)
async def start_profile_sync(background_tasks: BackgroundTasks, github_id: str):
    """유저의 최신 기술 스택과 활동 내역을 동기화합니다 (즉시 응답, 백그라운드 진행)"""
    task_id = str(uuid.uuid4())
    fake_db[task_id] = {"status": "pending"}
    background_tasks.add_task(background_sync_worker, task_id, github_id)
    return {
        "message": "데이터 분석이 백그라운드에서 시작되었습니다. 최대 1분 정도 소요됩니다.",
        "task_id": task_id
    }


@app.get("/api/profile/status/{task_id}")
async def check_sync_status(task_id: str):
    """현재 백그라운드 분석 진행 상황 또는 결과 JSON을 반환합니다"""
    task_info = fake_db.get(task_id)
    if not task_info:
        raise HTTPException(status_code=404, detail="해당 분석 작업(Task)을 찾을 수 없습니다.")
    s = task_info["status"]
    if s in ["pending", "processing"]:
        return {"status": s, "message": "열심히 유저의 코드를 분석하고 통계를 내고 있습니다... ⏳"}
    elif s == "failed":
        return {"status": "failed", "error": task_info.get("error_message")}
    elif s == "completed":
        return {
            "status": "completed",
            "message": "분석 완료! 프론트엔드 맞춤형 데이터를 전송합니다.",
            "data": task_info["data"]["profile"]
        }


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

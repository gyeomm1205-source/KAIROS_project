from contextlib import asynccontextmanager
from datetime import datetime, timezone

import uuid

from fastapi import FastAPI, Request, BackgroundTasks, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette import status

from app.api.internal.curriculum import router as internal_curriculum_router
from app.api.internal.quiz import router as internal_quiz_router
from app.api.internal.recommend import router as internal_recommend_router
from app.api.test import router as test_router
from app.schemas.common import ErrorResponse
from app.services.errors import AppError
from app.services.qdrant_client import close_qdrant_client, get_qdrant_client, init_collections
from app.services.profile_analyzer import start_github_collection, start_velog_and_analysis
from pydantic import BaseModel


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
app.include_router(internal_curriculum_router)
app.include_router(internal_quiz_router)
app.include_router(internal_recommend_router)

# 데모용 인메모리 저장소 (실제로는 DB로 교체 필요)
fake_db = {}


class GithubCollectRequest(BaseModel):
    userId: int
    githubToken: str
    githubUsername: str

class ProfileAnalyzeRequest(BaseModel):
    userId: int
    velogUsername: str
    githubTaskId: str

async def github_collect_worker(task_id: str, req: GithubCollectRequest):
    """GitHub 사전 수집 백그라운드 워커"""
    try:
        fake_db[task_id] = {"status": "processing", "data": None}
        # 본래는 req에서 받은 파라미터를 넘겨야 하지만 구조상 현재는 기존 함수 사용
        github_context = await start_github_collection()
        fake_db[task_id]["status"] = "completed"
        fake_db[task_id]["data"] = {"github_context": github_context}
    except Exception as e:
        fake_db[task_id]["status"] = "failed"
        fake_db[task_id]["error_message"] = str(e)

async def profile_analyze_worker(task_id: str, req: ProfileAnalyzeRequest):
    """Velog 수집 및 LLM 통합 분석 워커"""
    try:
        fake_db[task_id] = {"status": "processing", "data": None}

        # 이전 GitHub 수집 결과 가져오기 (fake_db)
        github_info = fake_db.get(req.githubTaskId)
        github_context = None
        if github_info and github_info["status"] == "completed":
            github_context = github_info["data"].get("github_context")

        results = await start_velog_and_analysis(github_context)
        fake_db[task_id]["status"] = "completed"
        fake_db[task_id]["data"] = {
            "profile": results["profile"],
            "activities": results["activities"]
        }
    except Exception as e:
        fake_db[task_id]["status"] = "failed"
        fake_db[task_id]["error_message"] = str(e)


@app.post("/api/v1/ai/github/collect-async", status_code=202)
async def trigger_github_collect(req: GithubCollectRequest, background_tasks: BackgroundTasks):
    """GitHub 데이터 사전 비동기 수집 트리거"""
    task_id = "task_" + str(uuid.uuid4()).replace("-", "")[:6]
    fake_db[task_id] = {"status": "pending"}
    background_tasks.add_task(github_collect_worker, task_id, req)
    return {
        "message": "GitHub Background collection started.",
        "taskId": task_id
    }


@app.post("/api/v1/ai/profile/analyze-async", status_code=202)
async def trigger_profile_analyze(req: ProfileAnalyzeRequest, background_tasks: BackgroundTasks):
    """Velog 수집 및 LLM 프로필 분석 비동기 시작"""
    task_id = "task_" + str(uuid.uuid4()).replace("-", "")[:6]
    fake_db[task_id] = {"status": "pending"}
    background_tasks.add_task(profile_analyze_worker, task_id, req)
    return {
        "message": "Velog Collection and AI Analysis started.",
        "taskId": task_id
    }


@app.get("/api/v1/ai/status/stream/{taskId}")
async def check_sync_status(taskId: str):
    """현재 백그라운드 분석 진행 상황을 확인합니다 (임시 JSON 반환 방식)"""
    task_info = fake_db.get(taskId)
    if not task_info:
        raise HTTPException(status_code=404, detail="해당 분석 작업(Task)을 찾을 수 없습니다.")
    s = task_info["status"]
    if s in ["pending", "processing"]:
        return {"status": s, "message": "열심히 유저의 데이터를 수집/분석하고 통계를 내고 있습니다... ⏳"}
    elif s == "failed":
        return {"status": "failed", "error": task_info.get("error_message")}
    elif s == "completed":
        return {
            "status": "completed",
            "message": "작업 완료!",
            "data": task_info.get("data")
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

from contextlib import asynccontextmanager
from datetime import datetime, timezone
import uuid
import httpx
import os

from fastapi import FastAPI, Request, BackgroundTasks, HTTPException
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from starlette import status

from app.api.internal.curriculum import router as internal_curriculum_router
from app.api.internal.quiz import router as internal_quiz_router
from app.api.internal.recommend import router as internal_recommend_router
from app.api.internal.daily_recommend import router as daily_recommend_router
from app.api.internal.feedback import router as feedback_router
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

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(test_router)
app.include_router(internal_curriculum_router)
app.include_router(internal_quiz_router)
app.include_router(internal_recommend_router)
app.include_router(daily_recommend_router)
app.include_router(feedback_router)

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
    callbackUrl: str

async def github_collect_worker(task_id: str, req: GithubCollectRequest):
    """GitHub 사전 수집 백그라운드 워커"""
    try:
        fake_db[task_id] = {"status": "processing", "data": None}
        # 본래는 req에서 받은 파라미터를 넘겨야 하지만 구조상 현재는 기존 함수 사용
        github_result = await start_github_collection(req.githubToken, req.githubUsername)
        fake_db[task_id]["status"] = "completed"
        fake_db[task_id]["data"] = {"github_result": github_result}  # dict {context, id_date_map}
    except Exception as e:
        fake_db[task_id]["status"] = "failed"
        fake_db[task_id]["error_message"] = str(e)

async def profile_analyze_worker(task_id: str, req: ProfileAnalyzeRequest):
    """Velog 수집 및 LLM 통합 분석 워커"""
    try:
        fake_db[task_id] = {"status": "processing", "data": None}

        # 이전 GitHub 수집 결과 가져오기 (fake_db)
        github_result = None
        if req.githubTaskId:
            github_info = fake_db.get(req.githubTaskId)
            if github_info and github_info["status"] == "completed":
                github_result = github_info["data"].get("github_result")  # dict 형식

        results = await start_velog_and_analysis(req.velogUsername, github_result)

        # id -> date 맵에서 날짜를 직접 조회 (LLM이 아닌 원본 데이터 기준)
        id_date_map = results.get("id_date_map", {})

        # 1. 활동 내역 매핑 (techScores 제거, summary로 통일)
        github_activities = []
        velog_activities = []
        for act in results["activities"]:
            act_type = act.get("type", "")
            mapped_type = "GITHUB_COMMIT"
            if "PR" in act_type:
                mapped_type = "GITHUB_PR"
            elif "Velog" in act_type:
                mapped_type = "VELOG_POST"

            # 원본 데이터에서 날짜 직접 조회 (LLM 불필요!)
            act_id = act.get("id")
            raw_date = id_date_map.get(act_id)
            if raw_date:
                try:
                    final_date = datetime.strptime(raw_date, "%Y-%m-%d").isoformat()
                except:
                    final_date = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")
            else:
                final_date = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")

            activity_dto = {
                "activityType": mapped_type,
                "summary": str(act.get("summary", ""))[:495],  # DB 길이 제한 방어
                "activityDate": final_date,
                "category": act.get("category", "학습"),
                "techStacks": act.get("tech_stacks", [])
            }
            if mapped_type in ["GITHUB_COMMIT", "GITHUB_PR"]:
                github_activities.append(activity_dto)
            else:
                velog_activities.append(activity_dto)
                
        payload = {
            "userId": req.userId,
            "taskId": task_id,
            "githubActivities": github_activities,
            "velogActivities": velog_activities
        }

        # 3. Spring Boot Webhook 호출
        spring_url = os.getenv("SPRING_SERVER_URL", "http://localhost:8080")

        try:
            async with httpx.AsyncClient() as client:
                # resp = await client.post(req.callbackUrl, json=payload, timeout=30.0)
                resp = await client.post(f"{spring_url}/analysis/complete", json=payload, timeout=30.0)
                resp.raise_for_status()
                print("✅ Spring Boot callback success:", resp.text)
        except Exception as http_err:
            print("❌ Spring Boot callback failed:", str(http_err))

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

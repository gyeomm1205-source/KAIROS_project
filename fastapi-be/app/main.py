from fastapi import FastAPI, BackgroundTasks, HTTPException
from pydantic import BaseModel
import asyncio
from app.services.profile_analyzer import run_integrated_analysis
import uuid

from app.api.internal.recommend import router as internal_recommend_router

# FastAPI 앱 생성
app = FastAPI(title="KAIROS AI Service")

# Internal routers (Spring Boot → FastAPI only)
app.include_router(internal_recommend_router)

# 실무에서의 진짜 DB 대신, 데모용 인메모리 저장소 (Dict)
# 실제로는 MongoDB, PostgreSQL 등에 유저 상태와 프로필을 저장해야 합니다.
fake_db = {}

# 1초 만에 응답 주기 위한 백그라운드 워커 함수
async def background_sync_worker(task_id: str, github_id: str):
    """실제 분석이 일어나는 (40초 걸리는) 백그라운드 함수"""
    try:
        # 상태를 '처리중'으로 변경
        fake_db[task_id] = {"status": "processing", "data": None}
        
        # 앞서 작성한 main_analyzer.py의 함수 호출! (모든 병렬화가 저 함수 안에 다 있습니다)
        # Note: 실무 연동 시 저 함수가 github_id, velog_id를 직접 인자로 받도록 수정되어야 합니다.
        results = await run_integrated_analysis() 
        
        # 분석이 성공적으로 끝나면 DB에 데이터를 꽂아넣고 상태를 '완료'로 변경
        fake_db[task_id]["status"] = "completed"
        fake_db[task_id]["data"] = {
            "profile": results["profile"],          # 프론트에 바로 뿌려줄 1페이지 요약 (방금 수정한 JSON!)
            "activities": results["activities"]     # 클릭하면 보여줄 100~200개의 세부 커밋 내역 리스트
        }
        
    except Exception as e:
        fake_db[task_id]["status"] = "failed"
        fake_db[task_id]["error_message"] = str(e)


# 프론트가 버튼을 눌렀을 때 호출하는 최초 API 
@app.post("/api/profile/sync", status_code=202)
async def start_profile_sync(background_tasks: BackgroundTasks, github_id: str):
    """유저의 최신 기술 스택과 활동 내역을 동기화합니다 (즉시 응답, 백그라운드 진행)"""
    
    # 1. 태스크 고유 ID 생성 (보통은 user_id로 관리)
    task_id = str(uuid.uuid4())
    fake_db[task_id] = {"status": "pending"}
    
    # 2. FastAPI의 BackgroundTasks에 긴 작업 쑤셔넣기
    background_tasks.add_task(background_sync_worker, task_id, github_id)
    
    # 3. 데이터나 LLM을 기다리지 않고 단 1초 만에 프론트로 즉시 응답 반환!
    # "분석 시작함! 이 task_id로 내 상태 확인하러 5초마다 와!"
    return {
        "message": "데이터 분석이 백그라운드에서 시작되었습니다. 최대 1분 정도 소요됩니다.",
        "task_id": task_id
    }


# 프론트가 로딩 화면에서 5초마다 계속 찌르는 폴링(Polling) API
@app.get("/api/profile/status/{task_id}")
async def check_sync_status(task_id: str):
    """현재 백그라운드 분석 진행 상황 또는 결과 JSON을 반환합니다"""
    
    task_info = fake_db.get(task_id)
    if not task_info:
        raise HTTPException(status_code=404, detail="해당 분석 작업(Task)을 찾을 수 없습니다.")
        
    status = task_info["status"]
    
    # 아직 분석 중일 때 (로딩 스피너 유지용)
    if status in ["pending", "processing"]:
        return {"status": status, "message": "열심히 유저의 코드를 분석하고 통계를 내고 있습니다... ⏳"}
        
    # 분석 실패 시
    elif status == "failed":
        return {"status": "failed", "error": task_info.get("error_message")}
        
    # ✨ 분석 완료 시! (DB에 저장된 프론트뷰 맞춤형 JSON을 그대로 던짐)
    elif status == "completed":
        return {
            "status": "completed",
            "message": "분석 완료! 프론트엔드 맞춤형 데이터를 전송합니다.",
            "data": task_info["data"]["profile"] 
        }

if __name__ == "__main__":
    import uvicorn
    # uvicorn api:app --reload 로 실행 가능
    uvicorn.run(app, host="127.0.0.1", port=8000)

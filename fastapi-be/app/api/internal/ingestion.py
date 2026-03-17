"""
관리자용 ingestion 트리거 엔드포인트.
Spring Boot가 아닌 내부 운영 목적으로만 사용합니다.
"""
from fastapi import APIRouter, BackgroundTasks
from pydantic import BaseModel

from app.services.ingestion.pipeline import run_ingestion
from app.services.qdrant_client import get_qdrant_client
from app.core.settings import QDRANT_COLLECTION_NAME

router = APIRouter(prefix="/internal/ingestion", tags=["ingestion"])


class IngestionResponse(BaseModel):
    message: str
    total_sources: int = 0
    total_points: int = 0
    elapsed_sec: float = 0.0


@router.post("/run", response_model=IngestionResponse)
async def trigger_ingestion(background_tasks: BackgroundTasks):
    """
    레퍼런스 문서 ingestion을 백그라운드로 실행합니다.
    즉시 응답 후 백그라운드에서 크롤 → 청크 → 임베딩 → 업로드를 수행합니다.
    """
    background_tasks.add_task(run_ingestion)
    return IngestionResponse(message="Ingestion 시작됨. 완료까지 수 분 소요될 수 있습니다.")


@router.get("/status")
async def ingestion_status():
    """현재 Qdrant reference_chunks 컬렉션의 포인트 수를 반환합니다."""
    client = get_qdrant_client()
    info = client.get_collection(QDRANT_COLLECTION_NAME)
    return {
        "collection": QDRANT_COLLECTION_NAME,
        "points_count": info.points_count,
        "vectors_count": info.vectors_count,
        "status": info.status,
    }

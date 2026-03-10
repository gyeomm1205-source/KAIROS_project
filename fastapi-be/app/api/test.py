from datetime import datetime, timezone

from fastapi import APIRouter
from fastapi.concurrency import run_in_threadpool
from starlette import status

from app.core.settings import QDRANT_COLLECTION_NAME
from app.schemas.ping import PingResponse
from app.services.errors import AppError
from app.services.qdrant_client import get_qdrant_client

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
    "/qdrant",
    response_model=PingResponse,
    summary="FastAPI to Qdrant ping",
)
async def ping_qdrant() -> PingResponse:
    client = get_qdrant_client()

    try:
        collections_response = await run_in_threadpool(client.get_collections)
        collection_exists = await run_in_threadpool(client.collection_exists, QDRANT_COLLECTION_NAME)
        collection_names = [collection.name for collection in collections_response.collections]
    except Exception as exc:
        raise AppError(
            status_code=status.HTTP_502_BAD_GATEWAY,
            code="QDRANT-001",
            message="Qdrant is unavailable.",
        ) from exc

    return PingResponse(
        source="fastapi-be",
        message="fastapi-to-qdrant-ok",
        timestamp=datetime.now(timezone.utc),
        downstream={
            "configuredCollection": QDRANT_COLLECTION_NAME,
            "collectionExists": collection_exists,
            "collections": collection_names,
        },
    )

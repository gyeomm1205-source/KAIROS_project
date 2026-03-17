from functools import lru_cache

from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams

from app.core.settings import QDRANT_HOST, QDRANT_PORT, QDRANT_COLLECTION_NAME

# 임베딩 벡터 차원 (OpenAI text-embedding-3-small 기준)
VECTOR_SIZE = 1536


@lru_cache
def get_qdrant_client() -> QdrantClient:
    return QdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)


def init_collections() -> None:
    """FastAPI 시작 시 컬렉션이 없으면 자동 생성."""
    client = get_qdrant_client()
    existing = {c.name for c in client.get_collections().collections}

    if QDRANT_COLLECTION_NAME not in existing:
        client.create_collection(
            collection_name=QDRANT_COLLECTION_NAME,
            vectors_config=VectorParams(
                size=VECTOR_SIZE,
                distance=Distance.COSINE,
            ),
        )
        print(f"[Qdrant] 컬렉션 생성: {QDRANT_COLLECTION_NAME}")
    else:
        print(f"[Qdrant] 컬렉션 확인 완료: {QDRANT_COLLECTION_NAME}")


def close_qdrant_client() -> None:
    client = get_qdrant_client()
    close = getattr(client, "close", None)
    if callable(close):
        close()
    get_qdrant_client.cache_clear()

from functools import lru_cache

from qdrant_client import QdrantClient

from app.core.settings import QDRANT_API_KEY, QDRANT_URL


@lru_cache
def get_qdrant_client() -> QdrantClient:
    return QdrantClient(
        url=QDRANT_URL,
        api_key=QDRANT_API_KEY,
    )


def close_qdrant_client() -> None:
    client = get_qdrant_client()
    close = getattr(client, "close", None)
    if callable(close):
        close()
    get_qdrant_client.cache_clear()

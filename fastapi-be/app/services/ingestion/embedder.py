"""
OpenAI text-embedding-3-small 모델로 텍스트 청크를 임베딩합니다.
GMS API(SSAFY 제공) 기반으로 동작합니다.
"""
import os

from openai import OpenAI

# GMS API 설정 (profile_analyzer.py와 동일)
_GMS_API_KEY = os.getenv("OPENAI_API_KEY", "S14P22A506-20649484-f08a-4522-a9fe-a26f5b4a9246")
_GMS_BASE_URL = os.getenv("OPENAI_API_BASE", "https://gms.ssafy.io/gmsapi/api.openai.com/v1")

EMBEDDING_MODEL = "text-embedding-3-small"
EMBEDDING_BATCH_SIZE = 20  # 한 번에 임베딩할 청크 수 (API 부하 조절)


def _get_client() -> OpenAI:
    return OpenAI(api_key=_GMS_API_KEY, base_url=_GMS_BASE_URL)


def embed_chunks(chunks: list[str]) -> list[list[float]]:
    """
    텍스트 청크 목록을 임베딩 벡터 목록으로 변환합니다.
    배치 단위로 요청해 API 부하를 조절합니다.
    """
    client = _get_client()
    vectors: list[list[float]] = []

    for i in range(0, len(chunks), EMBEDDING_BATCH_SIZE):
        batch = chunks[i : i + EMBEDDING_BATCH_SIZE]
        
        # JSON 직렬화 오류(400 Bad Request) 방지를 위한 텍스트 정제 (Null byte 및 깨진 유니코드 제거)
        safe_batch = [str(text).encode('utf-8', 'ignore').decode('utf-8').replace('\x00', '') for text in batch]
        
        print(f"    [Embedder] 배치 {i // EMBEDDING_BATCH_SIZE + 1}: {len(safe_batch)}개 임베딩 중...")
        resp = client.embeddings.create(model=EMBEDDING_MODEL, input=safe_batch)
        vectors.extend([item.embedding for item in resp.data])

    return vectors


def embed_text(text: str) -> list[float]:
    """단일 텍스트를 임베딩합니다."""
    client = _get_client()
    safe_text = str(text).encode("utf-8", "ignore").decode("utf-8").replace("\x00", "")
    resp = client.embeddings.create(model=EMBEDDING_MODEL, input=[safe_text])
    return resp.data[0].embedding

"""
청크 + 벡터를 Qdrant reference_chunks 컬렉션에 업로드합니다.
중복 doc_id는 건너뜁니다.
"""
import uuid

from qdrant_client.models import PointStruct, Filter, FieldCondition, MatchValue

from app.services.qdrant_client import get_qdrant_client
from app.core.settings import QDRANT_COLLECTION_NAME


def _existing_doc_ids() -> set[str]:
    """Qdrant에 이미 저장된 doc_id prefix 집합을 반환합니다."""
    client = get_qdrant_client()
    # scroll로 전체 payload 조회 (소규모 컬렉션 기준)
    records, _ = client.scroll(
        collection_name=QDRANT_COLLECTION_NAME,
        with_payload=["doc_id"],
        limit=10_000,
    )
    existing: set[str] = set()
    for r in records:
        if r.payload and "doc_id" in r.payload:
            # doc_id 형식: "source_001_chunk_N" → prefix는 "source_001"
            parts = r.payload["doc_id"].rsplit("_chunk_", 1)
            if parts:
                existing.add(parts[0])
    return existing


def upload_document(
    source: dict,
    chunks: list[str],
    vectors: list[list[float]],
) -> int:
    """
    하나의 문서(chunks + vectors)를 Qdrant에 업로드합니다.
    이미 존재하는 doc_id면 건너뜁니다.

    Returns:
        업로드된 포인트 수
    """
    client = get_qdrant_client()
    doc_prefix = _make_doc_prefix(source["url"])

    # 중복 체크
    existing = _existing_doc_ids()
    if doc_prefix in existing:
        print(f"    [Uploader] 이미 존재, skip: {doc_prefix}")
        return 0

    points: list[PointStruct] = []
    for i, (chunk, vector) in enumerate(zip(chunks, vectors)):
        point = PointStruct(
            id=str(uuid.uuid4()),
            vector=vector,
            payload={
                "doc_id": f"{doc_prefix}_chunk_{i}",
                "source_url": source["url"],
                "title": source.get("title", ""),
                "text": chunk,
                "skill": source.get("skill", []),
                "topic": source.get("topic", ""),
                "source_type": source.get("source_type", ""),
                "language": source.get("language", "ko"),
                "updated_at": source.get("updated_at", source.get("published_at", "")),
                "freshness_grade": source.get("freshness_grade", "stable"),
                "allowed": True,
            },
        )
        points.append(point)

    if points:
        client.upsert(collection_name=QDRANT_COLLECTION_NAME, points=points)

    return len(points)


def _make_doc_prefix(url: str) -> str:
    """URL을 안전한 doc_id prefix로 변환합니다."""
    # URL에서 도메인+경로만 추출, 특수문자 제거
    safe = url.replace("https://", "").replace("http://", "")
    safe = "".join(c if c.isalnum() or c in "_-" else "_" for c in safe)
    return safe[:80]  # 너무 길면 자름

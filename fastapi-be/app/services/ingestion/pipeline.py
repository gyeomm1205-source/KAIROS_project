"""
레퍼런스 문서 ingestion 전체 파이프라인.
크롤 → 청크 → 임베딩 → Qdrant 업로드 순서로 실행합니다.

직접 실행:
    python -m app.services.ingestion.pipeline
"""
import asyncio
import time

from app.services.ingestion.sources import SOURCES
from app.services.ingestion.crawler import crawl_all
from app.services.ingestion.chunker import split_into_chunks
from app.services.ingestion.embedder import embed_chunks
from app.services.ingestion.uploader import upload_document


async def run_ingestion(sources: list[dict] | None = None) -> dict:
    """
    전체 ingestion 파이프라인을 실행합니다.

    Args:
        sources: 수집할 source dict 목록. None이면 sources.py의 기본 목록 사용.

    Returns:
        {"total_sources": int, "total_points": int, "elapsed_sec": float}
    """
    if sources is None:
        sources = SOURCES

    start = time.time()
    total_points = 0

    print(f"\n[Ingestion] 시작 - {len(sources)}개 소스 처리 예정")

    # ─── 1. 크롤링 ─────────────────────────────────────────────────
    print("\n▶ 1단계: 크롤링")
    crawled = await crawl_all(sources)

    # ─── 2. 청크 + 임베딩 + 업로드 (소스 단위) ─────────────────────
    print("\n▶ 2단계: 청크 → 임베딩 → 업로드")
    for doc in crawled:
        url = doc["url"]
        raw_text = doc.get("raw_text", "")

        if not raw_text:
            print(f"  [skip] 텍스트 없음: {url}")
            continue

        # 청크 분할
        chunks = split_into_chunks(raw_text)
        if not chunks:
            print(f"  [skip] 청크 없음: {url}")
            continue

        print(f"  [{doc['title']}] {len(chunks)}개 청크")

        # 임베딩
        vectors = embed_chunks(chunks)

        # Qdrant 업로드
        uploaded = upload_document(doc, chunks, vectors)
        total_points += uploaded
        print(f"    → {uploaded}개 업로드 완료")

    elapsed = time.time() - start
    print(f"\n[Ingestion] 완료 - {total_points}개 포인트 업로드 / {elapsed:.1f}초 소요")

    return {
        "total_sources": len(sources),
        "total_points": total_points,
        "elapsed_sec": round(elapsed, 1),
    }


if __name__ == "__main__":
    asyncio.run(run_ingestion())

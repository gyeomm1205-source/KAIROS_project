"""
레퍼런스 문서 ingestion 전체 파이프라인 (2단계 구조).

1단계: discovery - 태그 페이지에서 글 목록(URL, 제목, 날짜, skill) 자동 수집
2단계: crawl - 각 URL 방문하여 본문 텍스트 수집
3단계: chunk → embed → upload to Qdrant

직접 실행:
    python -m app.services.ingestion.pipeline
"""
import asyncio
import time

from app.services.ingestion.seeds import ALL_SEEDS
from app.services.ingestion.discovery import discover_articles
from app.services.ingestion.crawler import crawl_all
from app.services.ingestion.chunker import split_into_chunks
from app.services.ingestion.embedder import embed_chunks
from app.services.ingestion.uploader import upload_document


async def run_ingestion(seeds: dict | None = None) -> dict:
    """
    전체 ingestion 파이프라인을 실행합니다.

    Args:
        seeds: 수집할 시드 딕셔너리. None이면 ALL_SEEDS 사용.

    Returns:
        {"total_articles": int, "total_points": int, "elapsed_sec": float}
    """
    if seeds is None:
        seeds = ALL_SEEDS

    start = time.time()
    total_points = 0

    # ─── 1단계: Discovery ────────────────────────────────────────────
    print("\n============================")
    print("▶ 1단계: 글 목록 자동 수집 (Discovery)")
    print("============================")
    articles = await discover_articles(seeds)

    if not articles:
        print("[Ingestion] 수집된 글 없음. 종료.")
        return {"total_articles": 0, "total_points": 0, "elapsed_sec": 0.0}

    # ─── 2단계: 본문 크롤링 ─────────────────────────────────────────
    print(f"\n============================")
    print(f"▶ 2단계: 본문 크롤링 ({len(articles)}개 글)")
    print("============================")
    crawled = await crawl_all(articles)

    # ─── 3단계: 청크 → 임베딩 → 업로드 ─────────────────────────────
    print(f"\n============================")
    print(f"▶ 3단계: 청크 → 임베딩 → Qdrant 업로드")
    print("============================")
    for doc in crawled:
        raw_text = doc.get("raw_text", "")
        if not raw_text:
            print(f"  [skip] 본문 없음: {doc.get('url', '')}")
            continue

        chunks = split_into_chunks(raw_text)
        if not chunks:
            print(f"  [skip] 청크 없음: {doc.get('title', '')}")
            continue

        print(f"  [{doc.get('title', '')[:35]}] {len(chunks)}개 청크 생성 완료. 임베딩 시작...", flush=True)
        vectors = embed_chunks(chunks)
        print(f"    → 임베딩 완료. Qdrant 업로드 중...", flush=True)
        uploaded = upload_document(doc, chunks, vectors)
        total_points += uploaded
        print(f"    → {uploaded}개 업로드 세이브 완료!", flush=True)

    elapsed = time.time() - start
    print(f"\n[Ingestion] 완료 - 총 {total_points}개 포인트 / {elapsed:.1f}초 소요")

    return {
        "total_articles": len(articles),
        "total_points": total_points,
        "elapsed_sec": round(elapsed, 1),
    }


if __name__ == "__main__":
    asyncio.run(run_ingestion())

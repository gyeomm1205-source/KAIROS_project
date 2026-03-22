import asyncio
from app.services.ingestion.discovery import discover_articles
from app.services.ingestion.crawler import crawl_all
from app.services.ingestion.seeds import SEEDS_NAVER

async def main():
    print("=== Testing Naver D2 Pipeline (Discovery + Crawler) ===")
    seeds = {"naver": SEEDS_NAVER}
    
    # 1. Discovery
    articles = await discover_articles(seeds)
    print(f"\n[Discovery] Found {len(articles)} unique Naver articles.")
    
    # 2. Crawler (테스트용으로 최신 1개만 실행)
    if articles:
        sample = [articles[0]]
        print(f"\n[Crawler] Testing 1 article: {sample[0]['url']}")
        crawled = await crawl_all(sample)
        print("\n--- Crawler Result ---")
        print(f"Title: {crawled[0].get('title')}")
        text = crawled[0].get('raw_text', '')
        print(f"Raw Text Preview: {text[:200]}...")
        print(f"Text Length: {len(text)} chars")

if __name__ == "__main__":
    asyncio.run(main())

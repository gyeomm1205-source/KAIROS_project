import asyncio
import os
import sys

# 프로젝트 루트를 경로에 추가
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from app.services.ingestion.discovery import discover_articles
from app.services.ingestion.crawler import crawl_all
from app.services.ingestion.summarizer import summarize_articles
from app.services.ingestion.seeds import SEEDS_TOSS

async def main():
    seeds = {"toss": SEEDS_TOSS}
    print("🚀 [1] Running discovery for Toss...")
    articles = await discover_articles(seeds)
    print(f"\n✅ Discovered {len(articles)} articles.")
    
    if articles:
        print("\n🚀 [2] Running crawler for first 2 articles...")
        crawled = await crawl_all(articles[:2])
        
        print("\n🚀 [3] Running summarizer & skill extractor...")
        summarized = await summarize_articles(crawled)
        
        for i, c in enumerate(summarized):
            print(f"\n--- [Article {i+1}] ---")
            print("URL:", c["url"])
            print("Title:", c["title"])
            print("Summary:", c.get("summary", ""))
            print("Skills(merged):", c.get("skill", []))
            print("-------------------------")

if __name__ == "__main__":
    asyncio.run(main())

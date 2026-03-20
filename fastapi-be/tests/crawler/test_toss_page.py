import asyncio
from curl_cffi.requests import AsyncSession
import json

async def main():
    async with AsyncSession(impersonate="chrome120") as session:
        # 토스 API 엔드포인트가 숨겨져 있는지 가장 흔한 경로 테스트
        test_urls = [
            "https://toss.tech/api/article",
            "https://toss.tech/api/articles",
            "https://toss.tech/api/v1/articles"
        ]
        print("--- Testing API Endpoints ---")
        for url in test_urls:
            resp = await session.get(url)
            print(f"[{resp.status_code}] {url} -> {resp.text[:50].strip()}")
            
        print("\n--- Testing Next.js Data Fetching (App Router _next/data) ---")
        # App Router의 JSON RSC 페이로드가 있는지 메인페이지 HTML에서 추적
        resp = await session.get("https://toss.tech/category/engineering")
        text = resp.text
        if "generateStaticParams" in text or "pagination" in text:
            print("Found some hint about pagination in HTML!")
        
        # HTML 상에서 "article" 개수 세어보기 
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(text, 'lxml')
        articles = soup.select('a[href^="/article/"]')
        print(f"Total article links in base HTML: {len(articles)}")

if __name__ == "__main__":
    asyncio.run(main())

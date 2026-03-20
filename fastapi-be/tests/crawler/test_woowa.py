import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/122.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
    "Cache-Control": "max-age=0",
    "Sec-Ch-Ua": "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"",
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": "\"Windows\""
}

async def main():
    async with AsyncSession(impersonate="chrome120") as session:
        resp = await session.get("https://techblog.woowahan.com/tag/spring/", headers=HEADERS, timeout=15)
        print(f"Status: {resp.status_code}")
        html = resp.text
        soup = BeautifulSoup(html, "lxml")
        
        # 전체 a 태그 중 글 링크처럼 보이는 것들 탐색
        print("--- Finding article links ---")
        articles = soup.find_all("a", href=True)
        count = 0
        for a in articles:
            href = a['href']
            # 보통 블로그 글 링크는 /숫자/ 형태가 포함되거나 특정한 클래스가 있음
            # 우아한형제들 최신 구조를 보면 h2 등 안에 있을 수 있음
            if 'techblog.woowahan.com' in href and len(href.split('/')) > 4:
                # 너무 많이 출력되지 않게 상위 부모 클래스나 구조 확인
                if a.find(['h1', 'h2', 'h3']) or (a.parent and a.parent.name in ['h1', 'h2', 'h3']):
                    print(f"Found link: {href}")
                    print(f"  Parent: {a.parent.name} class={a.parent.get('class')}")
                    count += 1
            elif a.find('h2'):
                print(f"Contains h2: {href}")
                count += 1
        
        print("\n--- Trying general article structures ---")
        items = soup.select("article") or soup.select(".post-item") or soup.select(".item")
        print(f"Found {len(items)} items using standard selectors.")
        for item in items[:2]:
            print(item.prettify()[:500])

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(main())

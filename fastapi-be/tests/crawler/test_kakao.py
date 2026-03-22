import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

async def main():
    # 카카오 테크 블로그 메인 포스트 목록 페이지
    url = "https://tech.kakao.com/blog"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    }
    
    print(f"[*] 카카오 기술블로그 접속 시도: {url}")
    async with AsyncSession(impersonate="chrome120") as session:
        resp = await session.get(url, headers=headers)
        if resp.status_code != 200:
            print(f"Error: HTTP {resp.status_code}")
            return
            
        soup = BeautifulSoup(resp.text, "lxml")
        
        # 타이틀 확인
        print("\n[사이트 타이틀]:", soup.title.get_text() if soup.title else "None")
        
        # 글 목록을 담고 있을 만한 컨테이너 찾기
        print("\n[게시물 링크 탐색]")
        # a 태그 중에 블로그 포스트로 가는 링크들을 대략 추출해봄
        links = soup.select("a[href*='/posts/'], a[href*='/blog/']")
        if not links:
            # 혹시 모르니 모든 h2, h3 출력해보기
            print("  -> 명시적인 /posts/ 형태의 A 태그 패턴이 안 보임. H2/H3 목록 출력:")
            for h in soup.find_all(['h2', 'h3'])[:5]:
                print(f"    - {h.name}: {h.get_text(strip=True).replace('\n', ' ')}")
        else:
            for a in links[:5]:
                print(f"  - 링크: {a.get('href')}")
                print(f"    텍스트: {a.get_text(strip=True).replace('\n', ' ')}")

if __name__ == "__main__":
    asyncio.run(main())

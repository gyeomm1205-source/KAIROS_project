import asyncio
from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

async def main():
    async with AsyncSession(impersonate="chrome120") as session:
        # 테스트용 단일 아티클 접속
        article_url = "https://toss.tech/article/harness-for-team-productivity"
        print(f"Fetching: {article_url}")
        
        resp = await session.get(article_url)
        print("Status:", resp.status_code)
        
        soup = BeautifulSoup(resp.text, 'lxml')
        
        # 1. 제목 파싱 테스트
        title = soup.find("title")
        print("\n=== Title ===")
        print(title.text if title else "No <title> found")
        
        # 2. 날짜 및 기타 정보 확인 (보통 span 들이나 time 태그에 있음)
        print("\n=== Meta Tags ===")
        meta_tags = soup.find_all("meta", property=lambda x: x and x.startswith("og:"))
        for m in meta_tags:
            print(f"{m.get('property')}: {m.get('content')}")
            
        # 3. 본문 텍스트 추출 확인
        print("\n=== Main Content Elements ===")
        # 불필요한 태그 제거 (스크립트, 헤더, 푸터 등)
        for tag in soup(["script", "style", "nav", "header", "footer"]):
            tag.decompose()
            
        # 보통 본문은 <article>이나 <main> 또는 특정 클래스가 있는 div에 있음
        main_content = soup.find("article") or soup.find("main") or soup.body
        
        if main_content:
            text_lines = []
            # h1~h4, p, li 등 위주로 추출
            for elem in main_content.find_all(["h1", "h2", "h3", "h4", "p", "li"]):
                text = elem.get_text(separator=" ", strip=True)
                if text:
                    text_lines.append(text)
            
            print(f"- Extracted {len(text_lines)} lines of text.")
            print("- First 5 lines:")
            for line in text_lines[:5]:
                print("  >", line)
            print("- Last 2 lines:")
            for line in text_lines[-2:]:
                print("  >", line)
        else:
            print("❌ No main content area found.")

if __name__ == "__main__":
    asyncio.run(main())

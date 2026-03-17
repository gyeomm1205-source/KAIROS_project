"""
URL에서 텍스트를 추출하는 크롤러.
aiohttp로 HTML을 가져오고, BeautifulSoup으로 본문 텍스트 추출.
"""
import asyncio
import re

import aiohttp
from bs4 import BeautifulSoup

# 요청 간 딜레이 (초) - robots.txt 준수
CRAWL_DELAY = 1.0
REQUEST_TIMEOUT = 15

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (compatible; KairosBot/1.0; "
        "+https://github.com/ssafy-kairos)"
    )
}


async def fetch_text(session: aiohttp.ClientSession, url: str) -> str:
    """단일 URL에서 본문 텍스트를 추출합니다."""
    try:
        async with session.get(
            url,
            headers=HEADERS,
            timeout=aiohttp.ClientTimeout(total=REQUEST_TIMEOUT),
            ssl=False,
        ) as resp:
            if resp.status != 200:
                print(f"  [Crawler] HTTP {resp.status}: {url}")
                return ""
            html = await resp.text(errors="replace")
    except Exception as e:
        print(f"  [Crawler] 요청 실패 ({url}): {e}")
        return ""

    return _extract_text(html)


def _extract_text(html: str) -> str:
    """HTML에서 의미 있는 텍스트만 추출합니다."""
    soup = BeautifulSoup(html, "lxml")

    # 불필요한 태그 제거
    for tag in soup(["script", "style", "nav", "header", "footer", "aside", "form"]):
        tag.decompose()

    # 본문 영역 우선 탐색
    main = (
        soup.find("article")
        or soup.find("main")
        or soup.find(class_=re.compile(r"post|content|article|entry", re.I))
        or soup.body
    )
    if main is None:
        return ""

    # 텍스트 추출
    lines = []
    for elem in main.find_all(["h1", "h2", "h3", "h4", "p", "li", "pre", "code"]):
        text = elem.get_text(separator=" ", strip=True)
        if text:
            lines.append(text)

    return "\n".join(lines)


async def crawl_all(sources: list[dict]) -> list[dict]:
    """
    sources 목록의 URL을 모두 크롤링하고,
    각 source dict에 'raw_text' 키를 추가해 반환합니다.
    """
    results = []
    async with aiohttp.ClientSession() as session:
        for i, source in enumerate(sources):
            url = source["url"]
            print(f"  [Crawler] ({i+1}/{len(sources)}) {url}")
            text = await fetch_text(session, url)
            results.append({**source, "raw_text": text})
            await asyncio.sleep(CRAWL_DELAY)
    return results

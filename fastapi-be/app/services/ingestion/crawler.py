"""
URL에서 텍스트를 추출하는 크롤러.
discovery.py가 수집한 글 목록을 입력받아 각 URL의 본문 텍스트를 추가합니다.
"""
import asyncio
import re

import aiohttp
from bs4 import BeautifulSoup

CRAWL_DELAY = 1.0
REQUEST_TIMEOUT = 15

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (compatible; KairosBot/1.0; "
        "+https://github.com/ssafy-kairos)"
    )
}


async def fetch_article(session: aiohttp.ClientSession, article: dict) -> dict:
    """
    단일 글(article dict)의 URL을 방문해서 본문 텍스트와 메타데이터를 추출합니다.
    article dict에 'raw_text', 'updated_at', 'skill'(태그에서 보강) 키를 추가해 반환합니다.
    """
    url = article["url"]
    try:
        async with session.get(
            url,
            headers=HEADERS,
            timeout=aiohttp.ClientTimeout(total=REQUEST_TIMEOUT),
            ssl=False,
        ) as resp:
            if resp.status != 200:
                print(f"  [Crawler] HTTP {resp.status}: {url}")
                return {**article, "raw_text": "", "updated_at": ""}
            html = await resp.text(errors="replace")
    except Exception as e:
        print(f"  [Crawler] 요청 실패 ({url}): {e}")
        return {**article, "raw_text": "", "updated_at": ""}

    soup = BeautifulSoup(html, "lxml")

    # 마지막 수정일 (meta 태그에서 추출)
    updated_at = _extract_updated_at(soup)

    # 본문 텍스트 추출 (article 본문 기준)
    raw_text = _extract_text(soup)

    # 글 본문 하단의 기술 태그로 skill 보강 (우아한형제들 기준)
    page_tags = _extract_skill_tags(soup)
    skill = list(dict.fromkeys(article.get("skill", []) + page_tags))  # 중복 제거

    return {
        **article,
        "raw_text": raw_text,
        "updated_at": updated_at,
        "skill": skill,
    }


def _extract_updated_at(soup: BeautifulSoup) -> str:
    """meta 태그에서 마지막 수정일 추출."""
    for prop in ["article:modified_time", "og:updated_time"]:
        tag = soup.find("meta", property=prop)
        if tag and tag.get("content"):
            return tag["content"][:10]  # YYYY-MM-DD 만 반환
    return ""


def _extract_skill_tags(soup: BeautifulSoup) -> list[str]:
    """본문 하단 기술 태그(a.tag-tag) 추출 (우아한형제들 기준)."""
    tags = []
    for tag in soup.select("a.tag-tag"):
        text = tag.get_text(strip=True).lstrip("#").strip()
        if text:
            tags.append(text)
    return tags


def _extract_text(soup: BeautifulSoup) -> str:
    """HTML에서 의미 있는 본문 텍스트만 추출합니다."""
    # 불필요한 태그 제거
    for tag in soup(["script", "style", "nav", "header", "footer", "aside", "form"]):
        tag.decompose()

    # 본문 영역 우선 탐색
    main = (
        soup.find("div", class_="entry-content")
        or soup.find("article")
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


async def crawl_all(articles: list[dict]) -> list[dict]:
    """
    discovery.py가 수집한 글 목록을 받아 각 URL의 본문을 수집합니다.

    Returns:
        raw_text, updated_at, skill이 추가된 article dict 목록
    """
    results = []
    async with aiohttp.ClientSession() as session:
        for i, article in enumerate(articles):
            print(f"  [Crawler] ({i+1}/{len(articles)}) {article['title'][:40]}")
            result = await fetch_article(session, article)
            results.append(result)
            await asyncio.sleep(CRAWL_DELAY)
    return results

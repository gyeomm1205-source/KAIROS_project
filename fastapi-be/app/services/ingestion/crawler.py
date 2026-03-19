"""
URL에서 텍스트를 추출하는 크롤러.
discovery.py가 수집한 글 목록을 입력받아 각 URL의 본문 텍스트를 추가합니다.
"""
import asyncio
import re

from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

CRAWL_DELAY = 1.0
REQUEST_TIMEOUT = 15

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


async def fetch_article(session: AsyncSession, article: dict) -> dict:
    """
    단일 글(article dict)의 URL을 방문해서 본문 텍스트와 메타데이터를 추출합니다.
    article dict에 'raw_text', 'updated_at', 'skill'(태그에서 보강) 키를 추가해 반환합니다.
    """
    url = article["url"]
    try:
        resp = await session.get(
            url,
            headers=HEADERS,
            timeout=REQUEST_TIMEOUT,
        )
        if resp.status_code != 200:
            print(f"  [Crawler] HTTP {resp.status_code}: {url}")
            return {**article, "raw_text": "", "updated_at": ""}
        html = resp.text
    except Exception as e:
        print(f"  [Crawler] 요청 실패 ({url}): {e}")
        return {**article, "raw_text": "", "updated_at": ""}

    soup = BeautifulSoup(html, "lxml")

    # 본문 텍스트 추출 (article 본문 기준)
    raw_text = _extract_text(soup)

    # 글 본문 하단의 기술 태그로 skill 보강 (우아한형제들 기준)
    page_tags = _extract_skill_tags(soup)
    raw_skills = article.get("skill", []) + page_tags
    # 대소문자 통일(소문자) 및 중복 제거
    skill = list(dict.fromkeys(s.lower() for s in raw_skills))

    # discovery에서 가져온 원본 제목/날짜(published_at)를 무조건 신뢰
    title = article.get("title", "")
    if not title:
        title = _extract_title(soup)

    return {
        **article,
        "title": title,
        "raw_text": raw_text,
        "skill": skill,
    }


def _extract_skill_tags(soup: BeautifulSoup) -> list[str]:
    """본문 하단 기술 태그(a.tag-tag) 추출 (우아한형제들 기준)."""
    tags = []
    for tag in soup.select("a.tag-tag"):
        text = tag.get_text(strip=True).lstrip("#").strip()
        if text:
            tags.append(text)
    return tags


def _extract_title(soup: BeautifulSoup) -> str:
    """본문 페이지의 <title> 또는 <h1>, og:title에서 제목 추출"""
    og_title = soup.find("meta", property="og:title")
    if og_title and og_title.get("content"):
        # 보통 og:title은 사이트 이름 없이 깔끔함
        return og_title["content"].strip()
        
    h1 = soup.find("h1")
    if h1:
        return h1.get_text(strip=True)
        
    title_tag = soup.find("title")
    if title_tag:
        # title 태그에 보통 "제목 | 우아한형제들 기술블로그" 형태로 들어감
        return title_tag.get_text(strip=True).split("|")[0].strip()
        
    return ""


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
    async with AsyncSession(impersonate="chrome120") as session:
        for i, article in enumerate(articles):
            print(f"  [Crawler] ({i+1}/{len(articles)}) {article['title'][:40]}")
            result = await fetch_article(session, article)
            results.append(result)
            await asyncio.sleep(CRAWL_DELAY)
    return results

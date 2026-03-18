"""
태그 페이지에서 글 목록(URL, 제목, 날짜, skill)을 자동 수집하는 디스커버리 모듈.

우아한형제들 구조:
  - 글 목록: div.posts-list > a (각 a 태그가 글 하나를 통째로 감쌈)
  - 제목: h1 (wrapper 내부)
  - 날짜: p > span (wrapper 내부 상단, "Aug.19.2025" 형식)
  - 다음 페이지: a.nextpostslink
"""
import asyncio
import re
from datetime import datetime

import aiohttp
from bs4 import BeautifulSoup

from app.services.ingestion.seeds import ALL_SEEDS, BLOG_META

HEADERS ={
"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
}

REQUEST_TIMEOUT = 15
CRAWL_DELAY = 1.0
MAX_PAGES = 5  # 태그당 최대 수집 페이지 수


# ─── 블로그별 파서 ───────────────────────────────────────────────────

def _parse_woowa_list(html: str, skill: list[str]) -> list[dict]:
    """우아한형제들 태그 페이지에서 글 목록 파싱."""
    soup = BeautifulSoup(html, "lxml")
    articles = []

    for wrapper in soup.select("div.posts-list > a"):
        url = wrapper.get("href", "").strip()
        if not url:
            continue

        title_tag = wrapper.find("h1")
        title = title_tag.get_text(strip=True) if title_tag else ""

        # 날짜: "Aug.19.2025" → "2025-08-19"
        date_str = ""
        span = wrapper.select_one("p > span")
        if span:
            date_str = _parse_woowa_date(span.get_text(strip=True))

        articles.append({
            "url": url,
            "title": title,
            "skill": skill,
            "published_at": date_str,
        })

    # 다음 페이지 링크
    next_page = soup.select_one("a.nextpostslink")
    next_url = next_page["href"] if next_page else None

    return articles, next_url


def _parse_woowa_date(raw: str) -> str:
    """'Aug.19.2025' → '2025-08-19'"""
    try:
        return datetime.strptime(raw, "%b.%d.%Y").strftime("%Y-%m-%d")
    except Exception:
        return raw


PARSERS = {
    "woowa": _parse_woowa_list,
}


# ─── 디스커버리 메인 ────────────────────────────────────────────────

async def _fetch_html(session: aiohttp.ClientSession, url: str) -> str:
    try:
        async with session.get(
            url,
            headers=HEADERS,
            timeout=aiohttp.ClientTimeout(total=REQUEST_TIMEOUT),
            ssl=False,
        ) as resp:
            if resp.status != 200:
                print(f"  [Discovery] HTTP {resp.status}: {url}")
                return ""
            return await resp.text(errors="replace")
    except Exception as e:
        print(f"  [Discovery] 요청 실패 ({url}): {e}")
        return ""


async def discover_articles(seeds: dict | None = None) -> list[dict]:
    """
    ALL_SEEDS를 순회하며 각 태그 페이지에서 글 목록을 수집합니다.

    Returns:
        list of {url, title, skill, published_at, source_type, language, freshness_grade}
    """
    if seeds is None:
        seeds = ALL_SEEDS

    all_articles: list[dict] = []

    async with aiohttp.ClientSession() as session:
        for blog_name, seed_list in seeds.items():
            if not seed_list:
                continue

            parser = PARSERS.get(blog_name)
            if parser is None:
                print(f"  [Discovery] {blog_name} 파서 없음, 스킵")
                continue

            meta = BLOG_META.get(blog_name, {})
            print(f"\n[Discovery] {blog_name} 수집 시작 ({len(seed_list)}개 태그)")

            for seed in seed_list:
                tag_url = seed["tag_url"]
                skill = seed["skill"]
                page_url = tag_url
                page_count = 0

                while page_url and page_count < MAX_PAGES:
                    print(f"  → {page_url}")
                    html = await _fetch_html(session, page_url)
                    if not html:
                        break

                    articles, next_url = parser(html, skill)

                    # 블로그 공통 메타데이터 병합
                    for a in articles:
                        a.update(meta)

                    all_articles.extend(articles)
                    print(f"     {len(articles)}개 발견 (누적 {len(all_articles)}개)")

                    page_url = next_url
                    page_count += 1
                    await asyncio.sleep(CRAWL_DELAY)

    # URL 중복 제거
    seen: set[str] = set()
    unique = []
    for a in all_articles:
        if a["url"] not in seen:
            seen.add(a["url"])
            unique.append(a)

    print(f"\n[Discovery] 완료 - 총 {len(unique)}개 고유 글 발견")
    return unique

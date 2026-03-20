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

from curl_cffi.requests import AsyncSession
from bs4 import BeautifulSoup

from app.services.ingestion.seeds import ALL_SEEDS, BLOG_META

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

REQUEST_TIMEOUT = 15
CRAWL_DELAY = 1.0
MAX_PAGES = 5  # 태그당 최대 수집 페이지 수


# ─── 블로그별 파서 ───────────────────────────────────────────────────

def _parse_woowa_list(html: str, skill: list[str]) -> list[dict]:
    """우아한형제들 태그 페이지에서 글 목록 파싱."""
    soup = BeautifulSoup(html, "lxml")
    articles = []

    # 변경된 구조: <div class="item"> 안에 <a> 태그로 감싸진 형태
    for item in soup.find_all("div", class_="item"):
        a_tag = item.find("a", href=True)
        if not a_tag:
            continue
            
        url = a_tag["href"].strip()
        if not url or 'techblog.woowahan.com' not in url or len(url.split('/')) < 4:
            continue

        # 보통 h2 안에 제목이 있음
        title_tag = a_tag.find("h2")
        title = title_tag.get_text(strip=True) if title_tag else ""

        # 날짜 구조: <p><span>Aug.19.2025</span></p> 등
        date_str = ""
        span = a_tag.find("span")
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


import json

def _parse_woowa_date(raw: str) -> str:
    """'Aug.19.2025' → '2025-08-19'"""
    try:
        return datetime.strptime(raw, "%b.%d.%Y").strftime("%Y-%m-%d")
    except Exception:
        return raw

def _parse_kakao_list(html_or_json: str, skill: list[str]) -> tuple[list[dict], str | None]:
    """카카오 기술블로그 API JSON 파싱."""
    articles = []
    try:
        data = json.loads(html_or_json)
        posts = data.get("postList", [])
        for post in posts:
            post_id = post.get("id")
            title = post.get("title", "")
            # "2026.03.16" 형식 변경 필요
            raw_date = post.get("releaseDate", "")
            date_str = raw_date.replace(".", "-") if raw_date else ""
            
            if post_id:
                articles.append({
                    "url": f"https://tech.kakao.com/posts/{post_id}",
                    "title": title,
                    "skill": skill,
                    "published_at": date_str,
                })
    except Exception as e:
        print(f"  [Discovery] 카카오 JSON 파싱 에러: {e}")
        
    return articles, None


def _parse_toss_list(xml_data: str, skill: list[str]) -> tuple[list[dict], str | None]:
    """토스 기술 블로그 RSS 피드 파싱."""
    # lxml-xml 파서 강제 사용
    soup = BeautifulSoup(xml_data, "xml")
    articles = []
    
    # RSS의 <item> 태그 단위로 파싱
    for item in soup.find_all("item"):
        link_tag = item.find("link")
        if not link_tag:
            continue
            
        url = link_tag.get_text(strip=True)
        if not url.startswith("http"):
            continue
            
        # 영문 번역본(-eng) 제거
        if url.endswith("-eng"):
            continue
            
        # RSS 내부의 실제 타이틀
        title_tag = item.find("title")
        title = title_tag.get_text(strip=True) if title_tag else ""
        
        # 발행일 (RSS 표준 pubDate 대소문자 구분)
        date_tag = item.find("pubDate")
        date_str = date_tag.get_text(strip=True) if date_tag else ""
        if date_str:
            try:
                from email.utils import parsedate_to_datetime
                dt = parsedate_to_datetime(date_str)
                date_str = dt.strftime("%Y-%m-%d")
            except Exception:
                pass
        
        articles.append({
            "url": url,
            "title": title,
            "skill": skill,
            "published_at": date_str,
        })
        
    return articles, None


def _parse_naver_list(json_str: str, skill: list[str]) -> tuple[list[dict], str | None]:
    """네이버 D2 기술 블로그 API 응답 파싱."""
    import json
    try:
        data = json.loads(json_str)
    except Exception:
        return [], None
        
    articles = []
    content_list = data.get("content", [])
    
    for item in content_list:
        raw_url = item.get("url", "")
        if not raw_url.startswith("http"):
            url = f"https://d2.naver.com{raw_url}"
        else:
            url = raw_url
            
        title = item.get("postTitle", item.get("title", ""))
        
        # Timestamp 변환
        pub_ts = item.get("postPublishedAt", 0)
        date_str = ""
        if pub_ts:
            from datetime import datetime
            try:
                date_str = datetime.fromtimestamp(pub_ts / 1000).strftime('%Y-%m-%d')
            except Exception:
                pass
            
        articles.append({
            "url": url,
            "title": title,
            "skill": skill,
            "published_at": date_str,
        })
        
    next_url = None
    if not data.get("last", True):
        current_page = data.get("number", 0)
        next_page = current_page + 1
        # API 다음 페이지 호출
        next_url = f"https://d2.naver.com/api/v1/contents?categoryId=2&page={next_page}&size=20"
        
    return articles, next_url


PARSERS = {
    "woowa": _parse_woowa_list,
    "kakao": _parse_kakao_list,
    "toss": _parse_toss_list,
    "naver": _parse_naver_list,
}


# ─── 디스커버리 메인 ────────────────────────────────────────────────

async def _fetch_html(session: AsyncSession, url: str) -> str:
    try:
        resp = await session.get(
            url,
            headers=HEADERS,
            timeout=REQUEST_TIMEOUT,
        )
        print(f"  [Debug] GET {url} -> Status: {resp.status_code}, Length: {len(resp.text)}")
        if resp.status_code != 200:
            print(f"  [Discovery] HTTP {resp.status_code}: {url}")
            return ""
        return resp.text
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

    async with AsyncSession(impersonate="chrome120") as session:
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

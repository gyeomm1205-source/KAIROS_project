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
    
    # [카카오 기술블로그 예외 처리]: 클라이언트 렌더링(CSR)을 우회하기 위해 본문 데이터도 상세 API로 직접 가져옵니다.
    if "kakao.com/posts/" in url:
        post_id = url.split("/posts/")[-1]
        api_url = f"https://tech.kakao.com/api/v1/posts/{post_id}"
        try:
            resp = await session.get(api_url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
            if resp.status_code == 200:
                data = resp.json()
                # 우선 post 딕셔너리 안에 content가 있는지, 아니면 최상단에 본문이 있는지 다양하게 탐색
                post_data = data.get("post", data)
                raw_text = post_data.get("content", post_data.get("body", ""))
                # 혹시 HTML 태그가 있다면 제거하고 텍스트만 추출
                if raw_text:
                    soup_api = BeautifulSoup(raw_text, "lxml")
                    raw_text = soup_api.get_text(separator=" ", strip=True)
                
                # 우아한형제들처럼 기술 스택 태시태그가 tags 리스트에 있을 수도 있으니 확인
                extra_skills = []
                tags = post_data.get("tags", [])
                for t in tags:
                    if isinstance(t, dict) and "name" in t:
                        extra_skills.append(t["name"].lower())
                    elif isinstance(t, str):
                        extra_skills.append(t.lower())
                
                final_skill = list(dict.fromkeys(article.get("skill", []) + extra_skills))
                
                return {
                    **article,
                    "title": post_data.get("title", article.get("title", "")),
                    "raw_text": raw_text,
                    "skill": final_skill,
                }
            else:
                print(f"  [Crawler] 카카오 API 에러: HTTP {resp.status_code}")
                return {**article, "raw_text": ""}
        except Exception as e:
            print(f"  [Crawler] 카카오 API 예외: {e}")
            return {**article, "raw_text": ""}

    # [네이버 D2 예외 처리]: 클라이언트 렌더링(CSR)을 우회하기 위해 본문 데이터를 단건 상세 API로 가져옵니다.
    if "d2.naver.com/helloworld/" in url:
        post_id = url.split("/helloworld/")[-1]
        api_url = f"https://d2.naver.com/api/v1/contents/{post_id}"
        try:
            resp = await session.get(api_url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
            if resp.status_code == 200:
                data = resp.json()
                # postTxt 필드에 텍스트가 바로 오거나, postHtml로 제공될 경우 대비
                raw_text = data.get("postTxt", data.get("postHtml", ""))
                if raw_text and "<" in raw_text:
                    soup_api = BeautifulSoup(raw_text, "lxml")
                    raw_text = soup_api.get_text(separator=" ", strip=True)
                
                return {
                    **article,
                    "title": data.get("postTitle", article.get("title", "")),
                    "raw_text": raw_text,
                }
            else:
                print(f"  [Crawler] 네이버 D2 API 에러: HTTP {resp.status_code}")
                return {**article, "raw_text": ""}
        except Exception as e:
            print(f"  [Crawler] 네이버 D2 API 예외: {e}")
            return {**article, "raw_text": ""}

    # [위키독스 예외 처리]: page-content 클래스에서 본문 추출
    if "wikidocs.net/" in url:
        try:
            resp = await session.get(url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
            if resp.status_code != 200:
                print(f"  [Crawler] 위키독스 HTTP {resp.status_code}: {url}")
                return {**article, "raw_text": ""}
            soup = BeautifulSoup(resp.text, "lxml")

            page_content = soup.find(class_="page-content")
            if page_content:
                # 불필요한 태그 제거
                for tag in page_content(["script", "style", "nav", "aside"]):
                    tag.decompose()
                lines = []
                for elem in page_content.find_all(["h1", "h2", "h3", "h4", "p", "li", "pre", "code", "td"]):
                    text = elem.get_text(separator=" ", strip=True)
                    if text:
                        lines.append(text)
                raw_text = "\n".join(lines)
            else:
                raw_text = _extract_text(soup)

            # "마지막 편집일시 : 2025년 2월 19일 10:42 오전" → "2025-02-19"
            published_at = ""
            date_div = soup.find("div", class_="muted")
            if date_div:
                date_match = re.search(r"(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일", date_div.get_text())
                if date_match:
                    y, m, d = date_match.groups()
                    published_at = f"{y}-{int(m):02d}-{int(d):02d}"

            return {**article, "raw_text": raw_text, "published_at": published_at}
        except Exception as e:
            print(f"  [Crawler] 위키독스 예외: {e}")
            return {**article, "raw_text": ""}

    # [공식문서 예외 처리]: Last-Modified 헤더 또는 meta/HTML에서 문서 수정일 추출
    if article.get("source_type") == "official_docs":
        try:
            resp = await session.get(url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
            if resp.status_code != 200:
                print(f"  [Crawler] 공식문서 HTTP {resp.status_code}: {url}")
                return {**article, "raw_text": ""}
            soup = BeautifulSoup(resp.text, "lxml")
            raw_text = _extract_text(soup)

            published_at = _extract_official_docs_date(resp, soup)

            return {**article, "raw_text": raw_text, "published_at": published_at}
        except Exception as e:
            print(f"  [Crawler] 공식문서 예외: {e}")
            return {**article, "raw_text": ""}

    # [나머지 일반 블로그]: 순수 HTML 파싱
    try:
        resp = await session.get(
            url,
            headers=HEADERS,
            timeout=REQUEST_TIMEOUT,
        )
        if resp.status_code != 200:
            print(f"  [Crawler] HTTP {resp.status_code}: {url}")
            return {**article, "raw_text": ""}
        html = resp.text
    except Exception as e:
        print(f"  [Crawler] 요청 실패 ({url}): {e}")
        return {**article, "raw_text": ""}

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


def _extract_official_docs_date(resp, soup: BeautifulSoup) -> str:
    """공식문서 페이지에서 문서 수정일을 추출한다. 우선순위: meta 태그 → HTML 요소 → HTTP 헤더."""
    # 1. meta 태그 (Docker 등)
    for prop in ["article:modified_time", "article:published_time", "og:updated_time"]:
        meta = soup.find("meta", property=prop)
        if meta and meta.get("content"):
            raw = meta["content"][:10]  # "2026-02-21 12:16:43..." → "2026-02-21"
            if re.match(r"\d{4}-\d{2}-\d{2}", raw):
                return raw

    # 2. HTML 요소 — "Last updated" 텍스트 근처 (Python 등)
    for tag in soup.find_all(string=re.compile(r"Last updated|Last modified", re.I)):
        parent = tag.parent
        if parent:
            date_match = re.search(r"(\d{4}-\d{2}-\d{2})", parent.get_text())
            if date_match:
                return date_match.group(1)
            # "Mar 22, 2026" 같은 형식
            date_match2 = re.search(
                r"([A-Z][a-z]{2})\s+(\d{1,2}),?\s+(\d{4})", parent.get_text()
            )
            if date_match2:
                from datetime import datetime
                try:
                    dt = datetime.strptime(date_match2.group(0).replace(",", ""), "%b %d %Y")
                    return dt.strftime("%Y-%m-%d")
                except ValueError:
                    pass

    # 3. HTTP Last-Modified 헤더 (Spring Boot 등)
    last_modified = resp.headers.get("Last-Modified", "")
    if last_modified:
        from email.utils import parsedate_to_datetime
        try:
            dt = parsedate_to_datetime(last_modified)
            return dt.strftime("%Y-%m-%d")
        except Exception:
            pass

    # 4. React.dev — GitHub API로 마지막 커밋 날짜 추출
    source_url = resp.url if hasattr(resp, 'url') else ""
    if "react.dev/" in str(source_url):
        return _fetch_react_date_from_github(str(source_url))

    return ""


def _fetch_react_date_from_github(page_url: str) -> str:
    """React.dev 페이지의 GitHub 소스 파일 마지막 커밋 날짜를 가져온다."""
    try:
        from curl_cffi.requests import Session
        from datetime import datetime

        path_part = page_url.split("react.dev/")[1].rstrip("/")
        file_path = f"src/content/{path_part}.md"
        api_url = f"https://api.github.com/repos/reactjs/react.dev/commits?path={file_path}&page=1&per_page=1"

        s = Session(impersonate="chrome120")
        resp = s.get(api_url, timeout=10)
        commits = resp.json()

        if isinstance(commits, list) and commits:
            date = commits[0]["commit"]["committer"]["date"]
            return datetime.strptime(date, "%Y-%m-%dT%H:%M:%SZ").strftime("%Y-%m-%d")
    except Exception as e:
        print(f"    [Crawler] React GitHub API 실패: {e}")

    return ""


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

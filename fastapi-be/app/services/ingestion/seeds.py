"""
블로그별 수집 시드(seed) 정의.
각 시드는 태그/카테고리 페이지 URL과 해당 기술 스택 정보를 담습니다.
discovery.py가 이 시드를 순회하며 글 목록을 자동 수집합니다.
"""

# ─── 우아한형제들 기술 블로그 ────────────────────────────────────────
SEEDS_WOOWA = [
    # 기존 백엔드/인프라
    {"tag_url": "https://techblog.woowahan.com/tag/spring/",       "skill": ["Spring Boot", "Spring"]},
    {"tag_url": "https://techblog.woowahan.com/tag/jpa/",          "skill": ["JPA", "Hibernate"]},
    {"tag_url": "https://techblog.woowahan.com/tag/mysql/",        "skill": ["MySQL", "Database"]},
    {"tag_url": "https://techblog.woowahan.com/tag/redis/",        "skill": ["Redis", "Cache"]},
    {"tag_url": "https://techblog.woowahan.com/tag/kafka/",        "skill": ["Kafka", "Message Queue"]},
    {"tag_url": "https://techblog.woowahan.com/tag/java/",         "skill": ["Java"]},
    {"tag_url": "https://techblog.woowahan.com/tag/docker/",       "skill": ["Docker", "Container"]},
    # 프론트엔드
    {"tag_url": "https://techblog.woowahan.com/tag/javascript/",   "skill": ["JavaScript", "Frontend"]},
    {"tag_url": "https://techblog.woowahan.com/tag/typescript/",   "skill": ["TypeScript", "Frontend"]},
    {"tag_url": "https://techblog.woowahan.com/tag/react/",        "skill": ["React", "Frontend"]},
    {"tag_url": "https://techblog.woowahan.com/tag/vue/",          "skill": ["Vue.js", "Frontend"]},
    {"tag_url": "https://techblog.woowahan.com/tag/css/",          "skill": ["CSS", "Frontend"]},
    # 앱 (iOS / Android)
    {"tag_url": "https://techblog.woowahan.com/tag/ios/",          "skill": ["iOS", "Swift", "Apple"]},
    {"tag_url": "https://techblog.woowahan.com/tag/android/",      "skill": ["Android", "Kotlin"]},
    # AI 및 데이터
    {"tag_url": "https://techblog.woowahan.com/tag/machine-learning/", "skill": ["Machine Learning", "AI"]},
    {"tag_url": "https://techblog.woowahan.com/tag/data/",         "skill": ["Data", "Data Engineering"]},
    {"tag_url": "https://techblog.woowahan.com/tag/python/",       "skill": ["Python", "Data"]},
]

# ─── 카카오 테크 블로그 ──────────────────────────────────────────────
# 카카오는 API를 통해 전체 게시글을 수집합니다. (1~10페이지)
SEEDS_KAKAO: list[dict] = [
    {"tag_url": f"https://tech.kakao.com/api/v1/posts?page={i}&code=blog", "skill": ["Kakao"]}
    for i in range(1, 11)
]

# ─── 토스 테크 블로그 ──────────────────────────────────────────────
# 토스는 RSS 피드를 통해 전체 게시글 목록을 제공합니다.
SEEDS_TOSS: list[dict] = [
    {"tag_url": "https://toss.tech/rss.xml", "skill": ["Toss"]},
]

# ─── 네이버 D2 ──────────────────────────────────────────────────────
# 네이버 D2 기술 블로그 (HelloWorld) 숨겨진 API
SEEDS_NAVER: list[dict] = [
    {"tag_url": "https://d2.naver.com/api/v1/contents?categoryId=2&page=0&size=20", "skill": ["Naver", "D2"]},
]

# ─── 위키독스 ──────────────────────────────────────────────────────
# 위키독스는 book 목차 페이지에서 javascript:page(ID) 링크를 파싱합니다.
SEEDS_WIKIDOCS: list[dict] = [
    {"tag_url": "https://wikidocs.net/book/1",      "skill": ["Python"]},
    {"tag_url": "https://wikidocs.net/book/4223",    "skill": ["Django", "Python"]},
    {"tag_url": "https://wikidocs.net/book/31",      "skill": ["Java"]},
    {"tag_url": "https://wikidocs.net/book/7601",    "skill": ["Spring Boot", "Java"]},
    {"tag_url": "https://wikidocs.net/book/8531",    "skill": ["FastAPI", "Python"]},
    {"tag_url": "https://wikidocs.net/book/4542",    "skill": ["Flask", "Python"]},
    {"tag_url": "https://wikidocs.net/book/2155",    "skill": ["Machine Learning", "AI", "Python"]},
]

# ─── 공식문서 ──────────────────────────────────────────────────────
# 공식문서는 목차/사이드바 페이지에서 하위 문서 링크를 파싱합니다.
SEEDS_OFFICIAL_DOCS: list[dict] = [
    {"tag_url": "https://docs.spring.io/spring-boot/reference/",        "skill": ["Spring Boot", "Java"]},
    {"tag_url": "https://react.dev/learn",                              "skill": ["React", "JavaScript"]},
    {"tag_url": "https://docs.python.org/3/tutorial/",                  "skill": ["Python"]},
    {"tag_url": "https://docs.docker.com/get-started/introduction/",    "skill": ["Docker", "Container"]},
]

# ─── 전체 시드 맵 ───────────────────────────────────────────────────
ALL_SEEDS: dict[str, list[dict]] = {
    "woowa": SEEDS_WOOWA,
    "kakao": SEEDS_KAKAO,
    "toss": SEEDS_TOSS,
    "naver": SEEDS_NAVER,
    "wikidocs": SEEDS_WIKIDOCS,
    "official_docs": SEEDS_OFFICIAL_DOCS,
}

# 블로그별 공통 메타데이터
BLOG_META: dict[str, dict] = {
    "woowa": {
        "source_type": "tech_blog_woowa",
        "language": "ko",
    },
    "kakao": {
        "source_type": "tech_blog_kakao",
        "language": "ko",
    },
    "toss": {
        "source_type": "tech_blog_toss",
        "language": "ko",
    },
    "naver": {
        "source_type": "tech_blog_naver",
        "language": "ko",
    },
    "wikidocs": {
        "source_type": "wiki",
        "language": "ko",
    },
    "official_docs": {
        "source_type": "official_docs",
        "language": "en",
    },
}

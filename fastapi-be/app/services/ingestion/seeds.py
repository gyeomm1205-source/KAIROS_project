"""
블로그별 수집 시드(seed) 정의.
각 시드는 태그/카테고리 페이지 URL과 해당 기술 스택 정보를 담습니다.
discovery.py가 이 시드를 순회하며 글 목록을 자동 수집합니다.
"""

# ─── 우아한형제들 기술 블로그 ────────────────────────────────────────
SEEDS_WOOWA = [
    {"tag_url": "https://techblog.woowahan.com/tag/spring/",       "skill": ["Spring Boot", "Spring"]},
    {"tag_url": "https://techblog.woowahan.com/tag/jpa/",          "skill": ["JPA", "Hibernate"]},
    {"tag_url": "https://techblog.woowahan.com/tag/mysql/",        "skill": ["MySQL", "Database"]},
    {"tag_url": "https://techblog.woowahan.com/tag/redis/",        "skill": ["Redis", "Cache"]},
    {"tag_url": "https://techblog.woowahan.com/tag/kafka/",        "skill": ["Kafka", "Message Queue"]},
    {"tag_url": "https://techblog.woowahan.com/tag/kotlin/",       "skill": ["Kotlin"]},
    {"tag_url": "https://techblog.woowahan.com/tag/java/",         "skill": ["Java"]},
    {"tag_url": "https://techblog.woowahan.com/tag/docker/",       "skill": ["Docker", "Container"]},
]

# ─── 카카오 테크 블로그 ──────────────────────────────────────────────
# 카카오는 JS 렌더링이 필요해 추후 추가 예정
SEEDS_KAKAO: list[dict] = []

# ─── 네이버 D2 ──────────────────────────────────────────────────────
# 네이버 D2도 구조 확인 후 추가 예정
SEEDS_NAVER: list[dict] = []

# ─── 전체 시드 맵 ───────────────────────────────────────────────────
ALL_SEEDS: dict[str, list[dict]] = {
    "woowa": SEEDS_WOOWA,
    "kakao": SEEDS_KAKAO,
    "naver": SEEDS_NAVER,
}

# 블로그별 공통 메타데이터
BLOG_META: dict[str, dict] = {
    "woowa": {
        "source_type": "tech_blog",
        "language": "ko",
    },
    "kakao": {
        "source_type": "tech_blog",
        "language": "ko",
    },
    "naver": {
        "source_type": "tech_blog",
        "language": "ko",
    },
}

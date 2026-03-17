"""
수집 대상 레퍼런스 문서 URL 목록.
각 항목은 pipeline.py에서 크롤링 대상으로 사용됩니다.

구조:
  - url: 크롤링할 페이지 URL
  - title: 문서 제목
  - skill: 관련 기술 태그 (Qdrant payload 필터링에 사용)
  - topic: 주제 분류
  - source_type: 출처 유형 (tech_blog / official_doc / tutorial)
  - language: 언어 (ko / en)
  - published_at: 게시일 (알 수 없으면 빈 문자열)
  - freshness_grade: stable / recent / outdated
"""

SOURCES = [
    # ─── 우아한형제들 기술 블로그 ──────────────────────────────────
    {
        "url": "https://techblog.woowahan.com/2663/",
        "title": "Spring Boot에서 Redis 캐시 다루기",
        "skill": ["Spring Boot", "Redis", "Cache"],
        "topic": "cache",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2021-07-05",
        "freshness_grade": "stable",
    },
    {
        "url": "https://techblog.woowahan.com/13101/",
        "title": "MySQL 인덱스 심화: 복합 인덱스와 실행 계획",
        "skill": ["MySQL", "Index", "Database"],
        "topic": "database",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2023-09-01",
        "freshness_grade": "stable",
    },
    {
        "url": "https://techblog.woowahan.com/2491/",
        "title": "Gradle 멀티 모듈 프로젝트 구성",
        "skill": ["Gradle", "Spring Boot", "Java"],
        "topic": "build",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2021-01-05",
        "freshness_grade": "stable",
    },
    # ─── 카카오 테크 블로그 ───────────────────────────────────────
    {
        "url": "https://tech.kakao.com/posts/351",
        "title": "실시간 서비스를 위한 Redis 활용",
        "skill": ["Redis", "Real-time", "Pub/Sub"],
        "topic": "cache",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2022-03-15",
        "freshness_grade": "stable",
    },
    {
        "url": "https://tech.kakao.com/posts/416",
        "title": "JVM 메모리 튜닝과 GC 최적화",
        "skill": ["Java", "JVM", "GC"],
        "topic": "performance",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2022-10-01",
        "freshness_grade": "stable",
    },
    # ─── 네이버 D2 ───────────────────────────────────────────────
    {
        "url": "https://d2.naver.com/helloworld/5827706",
        "title": "Spring WebFlux와 리액티브 프로그래밍",
        "skill": ["Spring WebFlux", "Reactive", "Java"],
        "topic": "reactive",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2020-08-01",
        "freshness_grade": "stable",
    },
    {
        "url": "https://d2.naver.com/helloworld/1230",
        "title": "Java HashMap은 어떻게 동작하는가",
        "skill": ["Java", "Data Structure"],
        "topic": "java",
        "source_type": "tech_blog",
        "language": "ko",
        "published_at": "2014-01-01",
        "freshness_grade": "stable",
    },
    # ─── WikiDocs ────────────────────────────────────────────────
    {
        "url": "https://wikidocs.net/160945",
        "title": "점프 투 스프링부트: JPA 시작하기",
        "skill": ["Spring Boot", "JPA", "Hibernate"],
        "topic": "orm",
        "source_type": "tutorial",
        "language": "ko",
        "published_at": "2023-01-01",
        "freshness_grade": "stable",
    },
    {
        "url": "https://wikidocs.net/21670",
        "title": "FastAPI 튜토리얼: 의존성 주입과 라우팅",
        "skill": ["FastAPI", "Python"],
        "topic": "web_framework",
        "source_type": "tutorial",
        "language": "ko",
        "published_at": "2022-06-01",
        "freshness_grade": "stable",
    },
    # ─── 공식 문서 ───────────────────────────────────────────────
    {
        "url": "https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html",
        "title": "Spring Security OAuth2 Login",
        "skill": ["Spring Security", "OAuth2"],
        "topic": "authentication",
        "source_type": "official_doc",
        "language": "en",
        "published_at": "",
        "freshness_grade": "stable",
    },
    {
        "url": "https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/",
        "title": "FastAPI OAuth2 with JWT",
        "skill": ["FastAPI", "JWT", "OAuth2"],
        "topic": "authentication",
        "source_type": "official_doc",
        "language": "en",
        "published_at": "",
        "freshness_grade": "stable",
    },
    {
        "url": "https://redis.io/docs/latest/develop/use/keyspace-notifications/",
        "title": "Redis Keyspace Notifications",
        "skill": ["Redis", "Pub/Sub"],
        "topic": "cache",
        "source_type": "official_doc",
        "language": "en",
        "published_at": "",
        "freshness_grade": "stable",
    },
]

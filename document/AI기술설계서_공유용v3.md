# AI 기술 설계서 v2

[기술 설계서 V2 ](https://www.notion.so/V2-31e16d89368680a0bfe8cf07d0834006?pvs=21)

## 0. AI 기술 설계 및 아키텍처 (팀 공유용 v3)

- 작성 기준일: 2026-03-09
- 적용 결정: **2안 채택**
    - **FE(Vue)는 FastAPI를 직접 호출하지 않는다.**
    - **Spring Boot가 단일 퍼블릭 백엔드 엔트리포인트**가 된다.
    - **FastAPI는 private/internal AI service**로만 동작한다.
    - **사용자/권한/OAuth/CRUD/외부 서비스 호출은 Spring Boot가 담당**한다.
    - **FastAPI는 도메인 DB(MySQL)를 직접 조회하지 않고**, Spring Boot가 조합한 AI 전용 입력 컨텍스트를 받아 AI 연산만 수행한다.

---

## 1. 배경과 문제 정의

KAIROS의 AI는 사용자의 `GitHub`, `Velog`, `Google Calendar` 데이터를 바탕으로 학습 맥락을 구조화하고, 다음 행동을 추천하는 개인 맞춤형 멘토 역할을 수행한다.

초기 설계에서는 **Spring Boot와 FastAPI를 동등한 백엔드 서버**처럼 두고, FE가 두 서버를 각각 호출할 수 있는 방향을 열어 두었다. 하지만 이 구조는 아래 문제를 만든다.

1. **인증/인가 경계가 모호해진다.**
    - 사용자 인증, OAuth 토큰, 권한 검증은 Spring Boot에 있는데, FE가 FastAPI를 직접 호출하면 인증 전략이 이원화된다.
2. **FastAPI가 사용자 정보를 알기 위해 Spring Boot를 다시 호출하는 비효율이 발생한다.**
    - 추천/퀴즈/커리큘럼 생성에 필요한 `learning_states`, 최근 활동, 일정 제약은 결국 Spring Boot가 가진 도메인 데이터다.
3. **서비스 소유권이 흐려진다.**
    - Spring Boot와 FastAPI가 같은 사용자 도메인을 서로 조회/수정하기 시작하면, 스키마 변경·권한 정책·장애 전파 범위가 복잡해진다.

따라서 본 문서는 위 문제를 해소하기 위해 **“FE → Spring Boot 단일 진입점, Spring Boot → FastAPI 내부 호출”** 구조를 최종안으로 확정하고, 그 기준에 맞춰 전체 설계를 다시 정의한다.

---

## 2. 최종 아키텍처 개요

### 2-1. 한눈에 보는 구조

```
Vue (Public UI)
  -> Spring Boot (Public API / Auth / OAuth / CRUD / Orchestration / External Integrations)
      -> MySQL
      -> GitHub / Velog / Google Calendar
      -> FastAPI (Private AI Service)
           -> LLM Provider
           -> Qdrant
      -> Calendar write-back / internal jobs / persistence
```

### 2-2. 핵심 원칙

1. **Single Public Entry Point**
    - FE는 항상 Spring Boot만 호출한다.
    - FastAPI는 외부에 공개하지 않는다.
2. **Domain Ownership = Spring Boot**
    - 사용자, OAuth 계정, 권한, CRUD, `user_activity`, `learning_states`, 추천/퀴즈/커리큘럼 저장은 Spring Boot가 소유한다.
3. **AI Responsibility = FastAPI**
    - LLM 호출, 구조화 추출, 임베딩, Qdrant 검색, 추천/퀴즈/커리큘럼 생성은 FastAPI가 담당한다.
4. **FastAPI 요청은 self-contained AIContext여야 한다.**
    - FastAPI에 `user_id`만 보내고 다시 Spring Boot를 조회하게 하지 않는다.
    - Spring Boot가 필요한 도메인 컨텍스트를 모두 조합해서 넘긴다.
5. **도메인 DB 직접 접근 금지**
    - FastAPI는 MySQL 사용자/도메인 테이블을 직접 조회/수정하지 않는다.
    - 필요 시 향후 `ai_context_snapshot` 같은 read model을 Spring Boot가 별도로 제공한다.
6. **구조화 JSON 우선**
    - 자연어 본문보다 파싱 가능한 JSON을 응답 표준으로 삼는다.
7. **장시간 작업 오케스트레이션은 Spring Boot가 담당**
    - 분석 작업 생성, 상태 관리, 재시도, 저장, FE 상태 전달은 Spring Boot가 맡는다.
    - FastAPI는 가급적 stateless compute service로 유지한다.

---

## 3. 컴포넌트별 책임 분리

| 컴포넌트 | 책임 |
| --- | --- |
| Vue | 온보딩, 분석 상태, 추천/퀴즈/커리큘럼 UI 렌더링. FastAPI 직접 호출 금지 |
| Spring Boot | 인증/인가, OAuth 연동, 외부 서비스 호출(GitHub/Velog/Google Calendar), 사용자 도메인 로직, MySQL 영속화, public API, 내부 작업 오케스트레이션 |
| FastAPI | 텍스트 마스킹, 구조화 추출, 임베딩, Qdrant 검색, 추천/퀴즈/커리큘럼 생성, 구조화 응답 포맷팅 |
| MySQL | 사용자/도메인 데이터, 분석 결과, 학습 상태, 추천/퀴즈/커리큘럼, 작업 상태 저장 |
| Qdrant | 레퍼런스 문서 임베딩 저장, 메타데이터 필터링 기반 검색 |
| LLM Provider | 구조화 추출, 추천 이유 생성, 퀴즈/커리큘럼 생성 |

### 3-1. Spring Boot가 소유하는 것

- 회원가입/로그인/세션/권한 검증
- GitHub, Velog, Google Calendar OAuth 토큰 저장 및 갱신
- 외부 서비스 API 호출 및 fetch 범위 제어
- `analysis_jobs`, `analysis_results`, `user_activity`, `learning_states` 저장
- 추천/퀴즈/커리큘럼 저장 및 사용자 수락/거절 처리
- 캘린더 반영 및 충돌 처리
- FE 응답용 public API 제공
- 내부 AI 호출용 client 및 orchestration

### 3-2. FastAPI가 소유하는 것

- 입력 텍스트 마스킹과 구조화 추출
- RAG/검색 질의 생성과 Qdrant retrieval
- 추천 근거, 퀴즈, 커리큘럼 생성
- 응답 스키마 검증과 최소 fallback 응답 생성
- 모델 라우팅, 프롬프트 버전 관리, AI 품질 고도화

### 3-3. FastAPI가 하지 않는 일

- FE 대상 public API 제공
- 사용자 권한/토큰 저장 및 해석
- MySQL 도메인 테이블 직접 조회/수정
- GitHub/Velog/Google Calendar 직접 OAuth 연동
- Google Calendar 최종 write-back
- 작업 상태 영속화의 source of truth 역할

---

## 4. 왜 2안을 채택하는가

### 4-1. 채택 이유

1. **인증/인가를 한 곳으로 고정할 수 있다.**
    - FE는 Spring Boot만 신뢰하면 되므로 보안 정책과 에러 처리 전략이 단순해진다.
2. **FastAPI가 사용자 정보를 다시 조회할 필요가 줄어든다.**
    - 추천/퀴즈/커리큘럼에 필요한 컨텍스트는 Spring Boot가 한 번에 조립해 넘길 수 있다.
3. **도메인 소유권이 분명해진다.**
    - MySQL 스키마, OAuth 토큰, 권한 정책은 Spring Boot만 책임진다.
    - FastAPI는 AI 품질에만 집중할 수 있다.
4. **FE/BE/AI 변경 영향 범위를 분리할 수 있다.**
    - AI 내부 프롬프트나 검색 전략을 바꿔도 FE와 Spring Boot 계약이 유지되면 영향 범위를 줄일 수 있다.
5. **운영 관점에서 안전하다.**
    - FastAPI를 private service로 두면 rate limit, 접근 제어, 내부 인증, 장애 격리가 쉬워진다.

### 4-2. 감수해야 하는 트레이드오프

1. **Spring Boot → FastAPI 추가 hop이 생긴다.**
    - 직접 호출보다 네트워크 비용과 latency가 소폭 증가한다.
2. **Spring Boot 오케스트레이션이 두꺼워질 수 있다.**
    - 외부 데이터 fetch, 도메인 조립, 저장, 캘린더 반영까지 맡으므로 application layer 설계가 중요해진다.
3. **내부 API 버전 관리가 필요하다.**
    - Spring Boot와 FastAPI 간 request/response schema를 명확히 유지해야 한다.

### 4-3. 완화 전략

- FastAPI 요청은 **단일 user_id가 아니라 완성된 AIContext**를 보내 chatty call을 줄인다.
- 장시간 분석은 Spring Boot가 background job으로 실행하고, FE는 Spring Boot job 상태만 조회한다.
- 동일 컨텍스트 조합에는 캐시를 적용해 반복 호출 비용을 줄인다.
- `trace_id`, `request_id`, `policy_version`, `prompt_version`을 표준 필드로 남겨 디버깅 가능성을 확보한다.

---

## 5. 핵심 AI 파이프라인 3가지

### 5-1. 활동 분석 파이프라인 (Spring Fetch + FastAPI Structured Extraction)

**핵심 아이디어:** 외부 서비스 호출은 Spring Boot가 하고, FastAPI는 전달받은 텍스트를 구조화한다.

- **Input:** Spring Boot가 GitHub/Velog/Google Calendar에서 가져온 원시/요약 텍스트
- **Process:**
    1. Spring Boot가 허용 범위 데이터를 fetch
    2. Spring Boot가 분석 job 생성
    3. FastAPI가 텍스트를 마스킹하고 구조화 JSON으로 추출
    4. Spring Boot가 결과를 저장하고 `learning_states`를 갱신
- **Output:** `user_activity`용 정규화 JSON, 기술 스택, 카테고리, 요약, confidence, 분석 경고

### 5-2. 맞춤형 레퍼런스 추천 파이프라인 (AIContext 기반 Retrieval + Generation)

**핵심 아이디어:** FastAPI는 Spring Boot가 넘긴 학습 상태와 최근 활동을 기반으로 Qdrant를 검색하고 추천 결과를 생성한다.

- **Input:** `learning_states[]`, `recent_activities[]`, `history_exclusions[]`, `calendar_constraints[]`, `target_positions[]`
- **Process:**
    1. FastAPI가 검색 질의와 메타데이터 필터를 생성
    2. Qdrant에서 Top-K 레퍼런스를 조회
    3. 추천 이유, 주의사항, assumptions를 구조화 JSON으로 생성
- **Output:** `items[]`, `reason`, `references[]`, `assumptions[]`, `outdated_warning`

### 5-3. 퀴즈/커리큘럼 생성 파이프라인 (Context-Aware Generation)

**핵심 아이디어:** 퀴즈와 커리큘럼은 현재 학습 상태와 일정 제약을 함께 고려한 행동 지침 생성이다.

- **Input:** 목표 기술, 최근 추천 레퍼런스, 학습 상태, 일정 제약, 학습 강도/톤
- **Process:**
    1. FastAPI가 구조화된 문제/로드맵 JSON 생성
    2. Spring Boot가 저장하고 사용자 수락/거절 상태를 반영
    3. 수락 시 Spring Boot가 Google Calendar 반영 작업으로 연결
- **Output:** `questions[]`, `rubric`, `missions[]`, `duration_days`, `why`, `references[]`

---

## 6. 상세 데이터 흐름

### 6-1. 최초 온보딩 분석

```
FE
  -> Spring Boot: 온보딩 완료, 외부 계정 연동 완료
Spring Boot
  -> GitHub / Velog / Google Calendar: 허용 범위 데이터 fetch
  -> analysis job 생성
  -> FastAPI /internal/analyze/activities 호출
FastAPI
  -> Sanitizer
  -> Structured Extractor
  -> 결과 JSON 반환
Spring Boot
  -> MySQL 저장 (user_activity, analysis_results, learning_states)
  -> job 상태 갱신
  -> FE에 분석 상태/결과 제공
```

### 처리 순서

1. FE가 온보딩 설문과 외부 계정 연동을 완료한다.
2. Spring Boot가 OAuth 토큰을 검증하고 외부 서비스 데이터를 fetch한다.
3. Spring Boot가 분석 대상 텍스트를 source item 단위로 정규화한다.
4. Spring Boot가 `analysis_jobs`에 작업을 생성한다.
5. Spring Boot background worker가 FastAPI `POST /internal/analyze/activities`를 호출한다.
6. FastAPI가 마스킹과 구조화 추출을 수행한다.
7. Spring Boot가 결과를 `user_activity`, `analysis_results`에 저장한다.
8. Spring Boot가 규칙/배치 기반으로 `learning_states`를 산정한다.
9. FE는 Spring Boot의 job 조회 API만 polling 한다.

### 6-2. 레퍼런스 추천

```
FE -> Spring Boot /api/v1/recommendations
Spring Boot
  -> learning_states, recent user_activity, profile, calendar constraints 조회
  -> AIContext 조합
  -> FastAPI /internal/recommendations 호출
FastAPI
  -> query generation
  -> Qdrant retrieval
  -> recommendation generation
Spring Boot
  -> recommendations 저장
  -> FE 응답
```

### 처리 원칙

- FastAPI는 MySQL을 다시 보지 않는다.
- Spring Boot는 추천 생성에 필요한 입력을 모두 채워서 한 번에 보낸다.
- FastAPI 응답은 최소 `reason`, `references`, `assumptions`를 포함해야 한다.

### 6-3. 퀴즈 생성

1. FE가 특정 기술 또는 추천 결과 기반 퀴즈 생성을 요청한다.
2. Spring Boot가 최근 활동, 현재 학습 수준, 선택 레퍼런스를 조합한다.
3. FastAPI가 `quiz_type`, `questions[]`, `rubric`, `references[]`를 생성한다.
4. Spring Boot가 결과를 저장하고 FE에 반환한다.

### 6-4. 커리큘럼 생성 및 수락 후 캘린더 반영

1. FE가 커리큘럼 생성 요청을 보낸다.
2. Spring Boot가 일정 제약, 목표 기술, 최근 추천 결과를 AIContext로 조합한다.
3. FastAPI가 `duration_days`, `nodes[]`, `why`, `references[]`를 구조화 JSON으로 반환한다.
4. Spring Boot가 초안을 저장한다.
5. 사용자가 수락하면 Spring Boot가 Google Calendar 반영 작업을 수행한다.
6. 일정 충돌 해결, 재시도, 최종 반영 상태 기록은 Spring Boot가 맡는다.

---

## 7. 데이터 소유권 및 저장 전략

### 7-1. 데이터 소유권 표

| 데이터/스토어 | 저장 위치 | 소유자 | 쓰기 주체 | 읽기 주체 |
| --- | --- | --- | --- | --- |
| `users`, `profiles`, `oauth_accounts` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| 외부 연동 메타데이터 | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `analysis_jobs`, `analysis_results` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `user_activity` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `learning_states` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `recommendations`, `reference_recommendations` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `quizzes`, `curriculums`, `calendar_sync_tasks` | MySQL | Spring Boot | Spring Boot | Spring Boot |
| `reference_chunks` | Qdrant | FastAPI/AI ingestion | FastAPI or 별도 ingestion job | FastAPI |
| AI 실행 메타데이터(`model`, `prompt_version`, `policy_version`) | MySQL | Spring Boot | Spring Boot(응답 수신 후 저장) | Spring Boot |

### 7-2. 원칙

- **MySQL 사용자/도메인 테이블은 Spring Boot 단독 소유**다.
- FastAPI는 Qdrant와 AI 내부 캐시 외에 도메인 persistence를 갖지 않는다.
- FastAPI가 필요한 도메인 정보는 Spring Boot가 request body로 전달한다.

### 7-3. 향후 확장: AI Read Model

MVP에서는 FastAPI의 MySQL 직접 접근을 허용하지 않는다. 다만 향후 아래 조건이 모두 충족되면 **Spring Boot가 materialize한 read-only AI snapshot**을 도입할 수 있다.

- 추천 latency가 병목이 되고
- 동일한 컨텍스트를 반복 조회해야 하며
- 도메인 스키마와 분리된 read model로 유지 가능할 때

예시:

- `ai_context_snapshots`
- `user_context_chunks`
- `recommendation_policy_cache`

이 경우에도 **원천 데이터의 source of truth는 Spring Boot**이며, FastAPI는 read-only로만 접근한다.

---

## 8. Spring Boot Public API 설계

FE는 아래 Spring Boot API만 사용한다.

### 8-1. `POST /api/v1/analysis/jobs`

- 목적: 온보딩 후 최초 분석 또는 재분석 시작
- 처리: Spring Boot가 job 생성 후 background worker로 내부 AI 호출 수행

### 8-2. `GET /api/v1/analysis/jobs/{jobId}`

- 목적: 분석 상태 조회
- 응답 예시 필드: `status`, `progress`, `activity_count`, `summary`, `skills`, `recommended_positions`

### 8-3. `POST /api/v1/recommendations`

- 목적: 학습 추천 또는 레퍼런스 추천 생성
- 특징: Spring Boot가 AIContext를 내부 조합한 후 FastAPI 호출

### 8-4. `POST /api/v1/quizzes`

- 목적: 사전/복습/면접형 퀴즈 생성

### 8-5. `POST /api/v1/curriculums`

- 목적: 일정 제약을 고려한 커리큘럼 초안 생성

### 8-6. `POST /api/v1/curriculums/{id}/accept`

- 목적: 커리큘럼 수락 후 캘린더 반영 요청
- 처리: Spring Boot가 캘린더 write-back 수행

---

## 9. Spring Boot ↔ FastAPI 내부 API 설계

### 9-1. 설계 원칙

- 내부 API는 **stateless compute interface**를 기본으로 한다.
- 작업 상태는 Spring Boot가 갖고, FastAPI는 계산 결과만 반환한다.
- 내부 API는 사용자 인증 대신 **service-to-service 인증**을 사용한다.
- 모든 요청에는 `request_id`, `trace_id`, `policy_version`을 포함한다.
- `user_id`는 correlation 용도로만 포함 가능하지만, **FastAPI가 이를 이용해 MySQL을 다시 조회해서는 안 된다.**

### 9-2. `POST /internal/analyze/activities`

- 목적: source item 묶음을 구조화 활동 JSON으로 변환
- 요청 예시

```
{
  "request_id":"analysis_20260309_001_batch_01",
  "trace_id":"trace-7f0f3f",
  "policy_version":"policy_v1",
  "user": {
    "user_id":101,
    "survey_context": {
      "job_role":"취준생",
      "target_positions": ["backend","ai"],
      "tech_stacks": ["Spring","FastAPI","Vue"],
      "persona":"default"
    }
  },
  "source_items": [
    {
      "source":"github",
      "source_item_id":"github:repo:team/kairos:README.md",
      "title":"README.md",
      "text":"Spring Security와 OAuth 연동을 구현한 프로젝트 설명...",
      "metadata": {
        "repo":"team/kairos",
        "updated_at":"2026-03-05T10:00:00Z"
      }
    },
    {
      "source":"velog",
      "source_item_id":"velog:post:12345",
      "title":"Spring Security OAuth 정리",
      "text":"OAuth2 로그인 흐름과 JWT 인증 필터를 정리한 글...",
      "metadata": {
        "published_at":"2026-03-01T12:30:00Z"
      }
    }
  ]
}
```

- 응답 예시

```
{
  "activities": [
    {
      "source":"velog",
      "source_item_id":"velog:post:12345",
      "category":"학습",
      "tech_stacks": ["Spring Security","OAuth","JWT"],
      "summary":"Spring Security와 OAuth 로그인 흐름을 정리한 글",
      "activity_date":"2026-03-01T12:30:00Z",
      "confidence":0.88
    }
  ],
  "analysis_summary": {
    "observed_strengths": ["Spring","OAuth"],
    "observed_gaps": ["JWT 심화","테스트 코드"],
    "recommended_positions": ["backend"]
  },
  "warnings": [
"calendar 데이터는 일정 제목만 사용되어 해석 정보가 제한적일 수 있음"
  ],
  "model_meta": {
    "model":"gpt-4o-mini",
    "prompt_version":"extract_v2"
  }
}
```

### 9-3. `POST /internal/recommendations`

- 목적: 학습 추천/레퍼런스 추천 생성
- 요청 예시

```
{
  "request_id":"rec_20260309_001",
  "trace_id":"trace-7f0f3f",
  "policy_version":"policy_v1",
  "user": {
    "user_id":101,
    "job_role":"취준생",
    "target_positions": ["backend","ai"]
  },
  "learning_states": [
    {
      "skill":"Spring Security",
      "raw_score":1.8,
      "relative_rank":"mid",
      "percentile":42
    },
    {
      "skill":"JWT",
      "raw_score":1.2,
      "relative_rank":"low",
      "percentile":28
    }
  ],
  "recent_activities": [
    {
      "source":"velog",
      "category":"학습",
      "tech_stacks": ["Spring Security","OAuth"],
      "summary":"OAuth 로그인 흐름을 정리한 글"
    }
  ],
  "history_exclusions": ["oauth_basic"],
  "calendar_constraints": [
    {
      "date":"2026-03-10",
      "busy_slots": ["19:00-21:00"]
    }
  ],
  "request_type":"reference"
}
```

- 응답 예시

```
{
  "items": [
    {
      "type":"reference",
      "title":"JWT Best Current Practices",
      "reason":"OAuth 기본 흐름은 익숙하므로, 현재 약한 JWT 심화 보완이 더 효과적임",
      "difficulty":"mid",
      "estimated_time":"25분"
    }
  ],
  "reason":"최근 OAuth 학습 흐름을 이어가되 낮은 점수의 JWT를 보강하기 위함",
  "assumptions": [
"이번 주 평일 저녁 3회 학습 가능",
"면접 준비를 병행 중"
  ],
  "references": [
    {
      "doc_id":"ref_jwt_001",
      "title":"JWT Best Current Practices",
      "url":"https://example.com/jwt-bcp",
      "freshness":"stable"
    }
  ],
  "model_meta": {
    "model":"gpt-4o",
    "prompt_version":"recommend_v3"
  }
}
```

### 9-4. `POST /internal/quizzes`

- 목적: 사전/복습/면접형 퀴즈 생성
- 핵심 입력: `target_skill`, `current_level`, `recent_references[]`, `quiz_type`, `difficulty`, `time_limit`
- 핵심 출력: `quiz_type`, `questions[]`, `rubric`, `references[]`, `model_meta`

### 9-5. `POST /internal/curriculums`

- 목적: 일정 제약을 고려한 커리큘럼 초안 생성
- 핵심 입력: `goals[]`, `learning_states[]`, `calendar_constraints[]`, `accepted_references[]`, `duration_days`
- 핵심 출력: `duration_days`, `nodes[]`, `why`, `assumptions[]`, `references[]`, `model_meta`

### 9-6. AIContext 조합 원칙

FastAPI 요청은 아래 원칙을 따른다.

1. `user_id`만 보내지 않는다.
2. 생성 판단에 필요한 데이터는 **Spring Boot가 사전에 join/aggregate**해서 보낸다.
3. 일정 제약, 최근 활동, 목표 포지션, 히스토리 제외 조건을 누락하지 않는다.
4. payload가 너무 크면 Spring Boot가 batch 또는 summary 형태로 분할한다.
5. 내부 API는 요청당 크기 제한을 둔다.
    - 예: `source_items` 10~20건 또는 총 텍스트 길이 50k~100k chars 이내

## 10. 구조화 출력 스키마 원칙

AI 응답은 자연어 본문보다 **구조화 JSON**을 우선한다.

### 10-1. 활동 정규화 스키마

```
{
  "source":"velog",
  "source_item_id":"velog:12345",
  "category":"학습",
  "tech_stacks": ["Spring Security","OAuth"],
  "summary":"Spring Security와 OAuth 흐름을 정리한 글",
  "activity_date":"2026-03-01T12:30:00Z",
  "confidence":0.84
}
```

### 10-2. 추천 결과 스키마

```
{
  "type":"reference",
  "reason":"최근 OAuth 학습 흐름을 이어가되 낮은 점수의 JWT를 보강하기 위함",
  "assumptions": [
"이번 주 평일 저녁 3회 학습 가능",
"면접 준비를 병행 중"
  ],
  "references": [
    {
      "doc_id":"ref_oauth_001",
      "url":"https://example.com/oauth-guide",
      "title":"OAuth 공식 가이드",
      "freshness":"warning"
    }
  ]
}
```

### 10-3. 퀴즈 스키마

```
{
  "quiz_type":"review",
  "questions": [
    {
      "id":1,
      "type":"short_answer",
      "question":"JWT의 signature가 필요한 이유를 설명하라",
      "expected_points": ["무결성 검증","위변조 방지"]
    }
  ],
  "rubric": {
    "full_score_criteria": ["핵심 개념을 정확히 설명","실무 맥락을 연결"]
  },
  "references": []
}
```

### 10-4. 커리큘럼 스키마

```
{
  "duration_days":5,
  "why":"JWT 심화와 테스트 코드 보완이 현재 목표 포지션 대비 우선순위가 높음",
  "assumptions": ["평일 저녁 위주 학습"],
  "nodes": [
    {
      "day":1,
      "title":"JWT 기본 개념 복습",
      "type":"study",
      "estimated_time":"40분"
    },
    {
      "day":2,
      "title":"인증 필터 테스트 코드 작성",
      "type":"action",
      "estimated_time":"60분"
    }
  ],
  "references": []
}
```

### 10-5. 응답 규칙

- 최소 공통 필드: `model_meta`, `references`, `assumptions` 또는 `warnings`
- 파싱 실패 시에도 최소 스키마는 유지해야 한다.
- Spring Boot는 FastAPI 응답 검증 후 저장한다.

---

## 11. Qdrant 설계

### 11-1. Qdrant를 본안으로 쓰는 이유

- 운영 환경에서 컬렉션 분리와 메타데이터 필터링이 명확하다.
- 문서 출처, 기술 스택, 게시 시점, 난이도, 언어 같은 payload 필터를 안정적으로 운용하기 쉽다.
- Spring Boot/MySQL과 독립된 검색 계층으로 두기 좋아 장애 격리가 용이하다.

### 11-2. MVP 컬렉션 전략

| 컬렉션 | 목적 |
| --- | --- |
| `reference_chunks` | 공식 문서, 검증된 기술 블로그, 내부 큐레이션 문서 청크 저장 |

### 11-3. 권장 payload 메타데이터

```
{
  "doc_id":"ref_oauth_001",
  "skill": ["OAuth","Spring Security"],
  "topic":"authentication",
  "source_type":"official_doc",
  "language":"ko",
  "published_at":"2024-07-18",
  "freshness_grade":"stable",
  "allowed":true
}
```

### 11-4. 검색 규칙

- `allowed = true` 문서만 검색 대상에 포함한다.
- 사용자 기술, 목표 포지션, 최근 활동 기술을 조합해 메타데이터 필터를 구성한다.
- 오래된 문서는 검색 후보에는 남기되 결과 출력 시 `freshness warning`을 명시한다.
- Spring Boot에서 받은 `learning_states + recent_activities + target_positions`를 질의 생성 입력으로 사용한다.

### 11-5. 향후 고도화

- `user_context_chunks` 컬렉션 추가
- hybrid retrieval(`keyword + dense`) 도입
- reranker 적용
- 쿼리 재작성(Query Rewriting) 및 freshness scoring 강화

---

## 12. Learning State 레벨 산정과 AI 활용

### 12-1. 소유권 원칙

- `learning_states` 계산과 저장의 source of truth는 Spring Boot다.
- FastAPI는 필요 시 기술 힌트나 해석 보조를 제공할 수 있지만, 최종 점수 산정과 저장은 Spring Boot가 담당한다.

### 12-2. 산정 대안

### 1안: 고정 임계값

- 예: `0.0~1.4 하`, `1.5~2.4 중`, `2.5 이상 상`
- 장점: 구현과 설명이 가장 단순하다.
- 단점: 기술별 표본 수 차이와 사용자 분포를 반영하지 못한다.

### 2안: 전체 사용자 백분위

- 같은 기술을 사용하는 전체 사용자 중 상위 몇 퍼센트인지 계산한다.
- 장점: 시장 비교 지표를 제공할 수 있다.
- 단점: 초기 서비스에서 표본 수가 적으면 왜곡될 수 있다.

### 3안: 개인 내 상대 비교

- 한 사용자 안에서 여러 기술의 점수를 비교해 강점과 약점을 나눈다.
- 장점: 개인 맞춤 추천, 보완 우선순위 설정에 적합하다.
- 단점: 전체 시장 대비 객관적 위치는 알려주지 못한다.

### 12-3. 권장안: 2 + 3 조합

- 추천 엔진 입력은 `raw score + 개인 내 상대 순위 + 기술별 전체 사용자 백분위`를 함께 사용한다.
- UI 역할 분리
    - 개인 내 상대 비교: “내 기술 중 무엇이 약한가”
    - 전체 백분위: “다른 사용자 대비 어느 정도인가”
- 추천 규칙 예시
    - 복습 추천: 개인 내 상대 하위 기술 우선
    - 도전 과제: 개인 내 상위 기술 중 전체 백분위도 높은 기술 우선
    - 면접 대비: 목표 포지션 핵심 기술 중 전체 백분위가 낮은 기술 우선

---

## 13. 보안 및 운영 규칙

### 13-1. 인증/권한

- OAuth access/refresh token은 **Spring Boot에만 저장**한다.
- FastAPI는 사용자 OAuth 토큰을 저장하거나 해석하지 않는다.
- FastAPI는 private network 또는 service mesh 내부에서만 접근 가능해야 한다.
- Spring Boot → FastAPI 호출은 service-to-service 인증(HMAC, internal JWT, mTLS 중 택1)을 사용한다.

### 13-2. 데이터 수집 범위 제어

- GitHub 수집 범위는 기본적으로 `README`, `package.json`, `requirements.txt`, `build.gradle`, `pom.xml` 같은 allowlist 파일로 제한한다.
- 코드 본문 전체 수집은 MVP에서 금지한다.
- Velog 본문과 Calendar description은 요약/마스킹 후 LLM 입력으로 전달한다.
- 민감 일정은 제목/시간만 사용하거나 정책상 제외한다.

### 13-3. 로그/추적

- 모든 요청은 `request_id`, `trace_id`, `user_id(마스킹 가능)`를 포함한다.
- 원문 텍스트 전체를 애플리케이션 로그에 남기지 않는다.
- FastAPI 응답의 `model`, `prompt_version`, `policy_version`, `references`는 Spring Boot가 저장한다.

### 13-4. 프라이버시 및 최소화 원칙

- LLM 입력에는 필요한 최소 텍스트만 전달한다.
- 원문 저장이 필요 없으면 요약본 또는 정규화 결과만 보관한다.
- 재분석 필요성이 큰 경우에만 별도 snapshot 저장소를 검토한다.

### 13-5. 운영 원칙

- FastAPI 장애가 나도 Spring Boot는 최소 fallback 응답 또는 재시도 상태를 FE에 전달할 수 있어야 한다.
- AI 정책 변경은 `policy_version` 단위로 추적 가능해야 한다.
- prompt/model 변경은 롤백 가능해야 한다.

---

## 14. 예외와 fallback

| 상황 | 대응 |
| --- | --- |
| GitHub/Velog/Calendar 연동 실패 | Spring Boot가 사용 가능한 소스만으로 분석 수행, 누락 소스를 UI에 명시 |
| 수집 데이터 부족 | 설문 기반 최소 추천으로 downgrade |
| FastAPI 호출 실패 | Spring Boot가 재시도 1회 후 fallback 응답 또는 “재분석 필요” 상태 반환 |
| Qdrant 검색 실패 | 최근 활동 + 큐레이션 기본 문서 세트 기반 추천으로 fallback |
| LLM 구조화 실패 | FastAPI 재시도 1회 후 규칙 기반 최소 스키마 반환 |
| 추천 근거 부족 | 추천 생성 중단 후 부족 사유를 명시하고 범위 확대 또는 재분석 유도 |
| 캘린더 반영 실패 | Spring Boot가 재시도하고 실패 사유를 기록, 사용자에게 수동 반영 안내 |

---

## 15. 폴더 구조 제안

### 15-1. Spring Boot 구조 제안

```
spring-app/
├── src/main/java/.../
│   ├── auth/                    # 인증/인가, OAuth, Security
│   ├── web/                     # Public REST Controller
│   ├── application/
│   │   ├── analysis/            # 분석 job orchestration
│   │   ├── recommendation/      # 추천 서비스
│   │   ├── quiz/                # 퀴즈 서비스
│   │   └── curriculum/          # 커리큘럼 서비스
│   ├── domain/                  # user_activity, learning_states 등 도메인 모델
│   ├── integration/
│   │   ├── github/              # GitHub fetch client
│   │   ├── velog/               # Velog fetch client
│   │   ├── calendar/            # Google Calendar fetch/write client
│   │   └── ai/                  # FastAPI internal client
│   ├── infrastructure/
│   │   ├── persistence/         # JPA/MyBatis 등
│   │   ├── scheduler/           # 재분석/배치/재시도
│   │   └── observability/       # trace/log/metrics
│   └── common/
└── resources/
```

### 15-2. FastAPI 구조 제안

```
fastapi-ai/
├── app/
│   ├── main.py
│   ├── api/
│   │   └── internal/
│   │       ├── analyze.py       # POST /internal/analyze/activities
│   │       ├── recommend.py     # POST /internal/recommendations
│   │       ├── quiz.py          # POST /internal/quizzes
│   │       └── curriculum.py    # POST /internal/curriculums
│   ├── core/
│   │   ├── config.py
│   │   ├── security.py          # service-to-service auth 검증
│   │   └── qdrant.py
│   ├── models/
│   │   ├── request.py
│   │   └── response.py
│   ├── services/
│   │   ├── sanitizer_svc.py
│   │   ├── extraction_svc.py
│   │   ├── retrieval_svc.py
│   │   ├── recommendation_svc.py
│   │   ├── quiz_svc.py
│   │   └── curriculum_svc.py
│   └── prompts/
│       ├── system_prompts.py
│       └── query_prompts.py
├── requirements.txt
└── .env
```

---

## 16. 기술 스택

### 16-1. Spring Boot

- `Spring Boot`
- `Spring Security`
- `Spring Web / WebClient`
- `JPA` 또는 `MyBatis`
- `Scheduler/Batch`
- `MySQL`

### 16-2. FastAPI

- `FastAPI`
- `Pydantic`
- `LangChain` 또는 경량 custom orchestration
- `qdrant-client`
- OpenAI API client

### 16-3. AI 모델 사용 전략

- 추출/분류/단순 요약: `gpt-4o-mini`
- 추천 이유/퀴즈/커리큘럼 생성: `gpt-4o` 또는 동급 모델
- 구조화 안정성이 중요한 작업은 schema-constrained output 우선

---

## 17. MVP 개발 마일스톤

### Week 1: 경계 확정 및 계약 정의

- FE → Spring Boot 단일 진입점 확정
- Spring Boot ↔ FastAPI 내부 API request/response schema 정의
- Spring Boot 외부 연동 fetch 모듈 초안 구현
- FastAPI `analyze/activities` 구조화 추출 구현

### Week 2: 분석 파이프라인 연결

- Spring Boot 분석 job 생성/상태 관리 구현
- GitHub/Velog/Calendar fetch → FastAPI 분석 → MySQL 저장 흐름 연결
- `user_activity` 저장 및 `learning_states` 산정 초안 구현
- Qdrant 컬렉션 세팅 및 reference ingestion 준비

### Week 3: 추천/퀴즈/커리큘럼 통합

- `recommendations`, `quizzes`, `curriculums` 내부 호출 구현
- Qdrant 검색 + 추천 근거 생성 연결
- 커리큘럼 수락 후 Google Calendar 반영 연결
- fallback, 구조화 검증, 관측성 지표 추가

---

## 18. 고도화 로드맵

### Phase 1. 검색 품질 향상

- `user_context_chunks` 도입
- hybrid retrieval(`keyword + dense`)
- reranker 적용
- Query Rewriting
- freshness scoring 강화

### Phase 2. 개인화 정밀도 향상

- 목표 포지션 중요도, 최근 학습 집중도, 퀴즈 정답률을 반영한 동적 가중치
- tone / complexity / pace 기반 적응형 프롬프트
- 반복 수락/거절 패턴 기반 장기 기억 요약

### Phase 3. 워크플로우 자동화

- analyze → retrieve → plan → critic → approve → publish 상태 그래프
- validator/critic 노드 추가
- 캘린더 반영 전 승인 단계 세분화
- 캐시와 비용 제어 고도화

### Phase 4. 심층 분석

- AST 기반 GitHub 분석
- 포트폴리오/블로그 초안 생성 연계
- 추천 explainability 패널 강화

### Phase 5. 부분적 serverless 활용(선택)

- 장시간 재분석, 임베딩 재생성, 백필(backfill) 작업에 한해 Lambda/Step Functions를 보조적으로 검토
- 단, **실시간 추천/퀴즈/커리큘럼의 주 경로는 본 설계(Spring Boot → FastAPI)를 유지**한다.

---

## 19. 품질 평가 및 운영 지표

### 19-1. 기준선 테스트 시나리오

| 시나리오 | 목적 | 입력 구성 |
| --- | --- | --- |
| 최초 분석 | Spring fetch + FastAPI 추출 + 저장 흐름 지연 시간 측정 | GitHub README 3건, Velog 글 5건, Calendar 일정 20건 |
| 레퍼런스 추천 | retrieval + generation 품질 측정 | 최근 활동 10건, learning state 8개 기술 |
| 퀴즈 생성 | 구조화 출력 안정성 측정 | 특정 기술 1개 + 최근 추천 문서 3건 |
| 커리큘럼 생성 | 일정 제약 고려 생성 안정성 측정 | 목표 기술 2개 + 캘린더 제약 7일 |
| fallback 테스트 | 장애 시 응답 안정성 측정 | FastAPI 실패, Qdrant 장애, 외부 소스 일부 누락 |

### 19-2. 측정 지표

| 분류 | 지표 |
| --- | --- |
| 지연 시간 | endpoint별 P50, P95, max latency |
| 안정성 | timeout rate, 5xx rate, 구조화 출력 파싱 성공률 |
| 검색 품질 | top-k hit rate, 근거 문서 적합도, outdated 문서 혼입률 |
| 생성 품질 | reason 포함률, reference 포함률, assumptions 포함률 |
| 비용 | 요청당 입력 토큰, 출력 토큰, 추정 비용 |
| 일관성 | 동일 입력 반복 실행 시 결과 변동 폭 |

### 19-3. 합격 기준 예시

- 구조화 출력 파싱 성공률: 98% 이상
- 추천/퀴즈/커리큘럼 생성 5xx 비율: 1% 이하
- fallback 경로 진입 시 최소 응답 스키마 보존율: 100%
- 고도화 후 P95 latency는 품질 개선이 없으면 기준선 대비 20% 이상 악화되지 않아야 한다.

### 19-4. 운영 중 Online Metric

- 추천 클릭률
- 추천 수락률
- 퀴즈 완주율
- 커리큘럼 완료율
- 재추천 성공률
- 평균 생성 시간
- 사용자당 AI 호출 비용

---

## 20. 비채택 대안 정리

### 20-1. FE → FastAPI 직접 호출

**비채택 이유**

- 인증/인가 정책이 이원화된다.
- FE가 두 백엔드 계약을 동시에 관리해야 한다.
- 사용자 도메인 컨텍스트를 FastAPI가 직접 알기 어렵다.

### 20-2. FastAPI의 MySQL 직접 조회/수정

**비채택 이유**

- Spring Boot와 FastAPI가 동일 도메인 스키마를 공동 소유하게 된다.
- 권한 정책, 마이그레이션, 장애 분석 범위가 복잡해진다.
- 단기적으로 편해 보여도 장기 변경 비용이 커진다.

### 20-3. FastAPI 제거 후 Lambda 전면 이전

**비채택 이유(현 시점)**

- 현재 문제의 본질은 compute 플랫폼보다 **도메인 경계** 문제다.
- 실시간 추천/퀴즈/커리큘럼은 항상 살아 있는 internal AI service가 더 단순하다.
- Lambda는 장시간/비동기 배치성 작업에는 유효하지만, 본 설계의 기본 경계를 대체하지는 않는다.

---

## 21. 미결/TBD

- Velog 수집 한계와 우회 전략 상세화
- Google Calendar 민감 일정 필터링 규칙 구체화
- reference allowlist 운영 주체와 갱신 주기
- 내부 API batch 크기 상한과 timeout 기준
- source snapshot 저장 필요 여부와 보관 기간
- 모델 선택 기준과 요청당 비용 상한선
- Spring Boot ↔ FastAPI service auth 방식(HMAC / internal JWT / mTLS) 최종 결정
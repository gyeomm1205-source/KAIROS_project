# KAIROS — AI 기반 개인화 개발자 학습 플랫폼

> GitHub와 Velog 활동을 분석하여 RAG 기반 맞춤형 학습 추천, AI 커리큘럼 생성, AI 퀴즈 생성을 제공하는 풀스택 학습 플랫폼

![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python_3.12-3776AB?style=for-the-badge&logo=python&logoColor=white)
![LangChain](https://img.shields.io/badge/LangChain-1C3C3C?style=for-the-badge&logo=langchain&logoColor=white)
![OpenAI](https://img.shields.io/badge/GPT--4o-412991?style=for-the-badge&logo=openai&logoColor=white)
![Qdrant](https://img.shields.io/badge/Qdrant-DC382D?style=for-the-badge&logo=qdrant&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue_3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 정보](#프로젝트-정보)
- [실행 방법](#실행-방법)

----

## 프로젝트 소개

### 배경

개발자가 성장하려면 **"지금 내가 어디에 있고, 다음에 뭘 공부해야 하는지"** 를 알아야 합니다.
하지만 대부분의 개발자는 자신의 기술 수준을 객관적으로 파악하기 어렵고, 수많은 학습 자료 중 자신에게 맞는 것을 고르기도 쉽지 않습니다.

### 해결 방법

KAIROS는 사용자의 **GitHub 커밋/PR**과 **Velog 블로그 글**을 자동 수집하고, 이를 벡터 DB에 임베딩하여 **RAG(Retrieval-Augmented Generation) 기반의 개인화된 학습 추천**을 제공합니다.

### 핵심 기능

| # | 기능 | 설명 |
|---|---|---|
| 1 | **활동 분석** | GitHub/Velog 데이터를 LLM으로 분석하여 기술 스택과 역량 프로필 자동 생성 |
| 2 | **학습 추천** | Qdrant 벡터 검색 + GPT-4o를 결합한 RAG 기반 맞춤 학습 자료 추천 |
| 3 | **커리큘럼 생성** | 3가지 모드(온보딩/자동/수동)로 날짜별 학습 계획 생성, Google Calendar 연동 |
| 4 | **퀴즈 생성** | 학습 내용 기반 퀴즈 자동 생성, Qdrant 청크 근거로 환각 방지 |
| 5 | **성장 추적** | 레이더/바 그래프로 기술별 성장 추이 시각화 |

---

## 기술 스택

| 분류 | 기술 | 선택 이유 |
|---|---|---|
| AI Backend | FastAPI, Python 3.12, LangChain | 비동기 처리에 강하고, LangChain 생태계와 자연스럽게 통합 |
| LLM | OpenAI GPT-4o / GPT-4o-mini | 구조화 출력 지원이 안정적이고, 한국어 성능 우수 |
| Vector DB | Qdrant | 메타데이터 필터링과 유사도 검색을 동시에 지원, 경량 배포 가능 |
| Embedding | text-embedding-3-small | 비용 대비 성능이 우수하고, 1536차원으로 충분한 표현력 |
| Main Backend | Spring Boot 4.0.2, Java 21, JPA, Spring Security | 도메인 로직, 인증, 데이터 영속성 담당 |
| Database | MySQL, Redis | RDBMS + 캐싱 레이어 |
| Frontend | Vue 3, Vite, Pinia, Three.js | 반응형 SPA + 랜딩 페이지 3D 효과 |
| Infra | Docker | 컨테이너 기반 수동 배포 |

---

## 주요 기능

### 1. GitHub/Velog 활동 분석
- GitHub REST API로 레포, 커밋, PR 데이터 수집
- Velog GraphQL API로 블로그 글 수집
- GPT-4o-mini로 활동 분류 (기술 스택, 카테고리, 요약)
- 최종 개발자 프로필 생성 (기술 역량, 추천 포지션)

### 2. RAG 기반 학습 추천
- 사용자 프로필에서 쿼리 텍스트 구성
- Qdrant에서 유사 학습 자료 벡터 검색
- GPT-4o가 검색 결과를 기반으로 개인화된 추천 이유, 현재 상태 진단, 다음 학습 경로 생성
- 일일 배치 추천 + 온디맨드 추천 지원

### 3. AI 커리큘럼 생성
- **온보딩 모드**: 프로필 분석 기반 초기 커리큘럼
- **자동 모드**: 최근 활동 기반, 약점 보완 / 강점 강화 두 가지 옵션 동시 생성
- **수동 모드**: 사용자 지정 주제로 커리큘럼 생성
- Google Calendar에서 기존 일정 조회 후, 비어있는 날짜에 학습 일정 배치 (2~7일)

### 4. AI 퀴즈 생성 (환각 방지)
- Qdrant에서 학습 주제 관련 청크 검색
- 검색된 청크만을 근거로 퀴즈 출제 (LLM이 자체 지식으로 문제를 만들지 못하도록 제약)
- 객관식, 주관식, 코딩 문제 3가지 유형
- 퀴즈와 채점 루브릭을 단일 LLM 호출로 동시 생성
- 사용자 수준(JUNIOR/MID/SENIOR)에 따라 난이도/유형 비율 자동 조정

### 5. 성장 추적
- 기술 스택별 레이더 차트로 역량 시각화
- 시간축 바 그래프로 학습 활동 추이 표시
- 가입 시점 기준선 대비 성장도 측정

---

## 프로젝트 정보

- **기간:** 2026.02 ~ 2026.04 (6주)
- **인원:** 5명
- **소속:** SSAFY (삼성 청년 SW 아카데미) 14기
- **분류:** 빅데이터 추천 프로젝트
- **도메인:** kairos.cloud-ip.cc

> 파트별 상세 개발 기록: [AI](docs/AI.md)

---

## 실행 방법

### Modules
- `vue-fe`: Vue 3 Frontend
- `springboot-be`: Spring Boot Main API
- `fastapi-be`: FastAPI AI Service

### Default ports
- Vue: `5173`
- Spring Boot: `8080`
- FastAPI: `8000`

### 1) Run FastAPI
```bash
cd fastapi-be
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
export SPRING_BASE_URL=http://localhost:8080
export FASTAPI_CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://kairos.cloud-ip.cc,http://kairos.cloud-ip.cc
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2) Run Spring Boot
```bash
cd springboot-be
chmod +x gradlew
export APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://127.0.0.1:5173,https://kairos.cloud-ip.cc,http://kairos.cloud-ip.cc
export APP_FASTAPI_BASE_URL=http://localhost:8000
./gradlew bootRun
```

### 3) Run Vue
```bash
cd vue-fe
cp .env.example .env
npm install
npm run dev
```

### Domain-ready config (`kairos.cloud-ip.cc`)
- Spring CORS default allows `https://kairos.cloud-ip.cc` and `http://kairos.cloud-ip.cc`.
- FastAPI CORS default allows `https://kairos.cloud-ip.cc` and `http://kairos.cloud-ip.cc`.
- Vue production example file: `vue-fe/.env.production.example`
- If reverse proxy routes differ by path, update:
  - `VITE_SPRING_BASE_URL`
  - `VITE_FASTAPI_BASE_URL`
  - `APP_FASTAPI_BASE_URL`
  - `SPRING_BASE_URL`

### Docs
- Spring Swagger: http://localhost:8080/swagger-ui/index.html
- FastAPI Swagger: http://localhost:8000/docs
- Vue Dev: http://localhost:5173/


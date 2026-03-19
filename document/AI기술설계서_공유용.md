# AI 기술 설계 및 아키텍처 (팀 공유용 v2)

## 1. 프로젝트 아키텍처 개요 (3-Tier)

본 프로젝트는 **FE(Vue) - BE(Spring/Node 등) - AI(Python FastAPI)** 의 3티어(Tier) 구조로 분리하여 개발합니다.

- **FE (Vue):** 사용자 UI/UX (3단 레이아웃, 캘린더 렌더링 등)
- **BE (Main Server):** 유저 인증(OAuth), RDB(PostgreSQL/MySQL) CRUD, 외부 API(GitHub/Velog/Calendar) 데이터 Fetch
- **AI (FastAPI Server):** 프롬프트 엔지니어링, 구조화된 데이터 추출, Qdrant 벡터 검색(RAG), 퀴즈/커리큘럼 생성 전담

## 2. 핵심 AI 파이프라인 3가지

AI 서버(FastAPI)가 담당할 핵심 파이프라인은 기존의 복잡한 구조를 탈피하여, 빠르고 정확한 최신 LLM 트렌드에 맞춰 재설계되었습니다.

### 1️⃣ 데이터 추출 파이프라인 (Single LLM Pass)

*빠르고 저렴한 LLM을 사용해 한 번에 데이터를 정형화합니다.*

- **Input:** BE가 넘겨준 원시 텍스트 (Velog 본문, GitHub README/package.json, 구글 캘린더 일정)
- **Process:** `gpt-4o-mini` 모델의 **Structured Output (구조화된 출력)** 기능 활용
- **Output (JSON):**
    
    ```
    {
      "tech_stacks": ["React", "TypeScript"],
      "summary": "React Query를 활용한 서버 상태 관리 개념 정리",
      "category": "학습" // (일상, 개발, 학습, 취준 중 택 1)
    }
    ```
    
- **Action:** 이 결과를 BE로 반환하면, BE가 RDB의 `user_activity`와 `learning_states`에 저장 및 점수화.

### 2️⃣ 맞춤형 레퍼런스 추천 파이프라인 (Self-Querying RAG)

*사용자가 검색어를 입력하지 않아도, LLM이 알아서 유저 상태를 분석해 메타데이터 필터링 쿼리를 생성합니다.*

- **Vector DB:** **Qdrant** (압도적인 메타데이터 필터링 성능 제공)
- **Input:** `learning_states`(스킬 점수) + `user_activity`(최근 활동 맥락 요약 리스트)
- **Process (LangChain `SelfQueryRetriever` 활용):**
    1. AI가 유저 상태를 보고 **스스로 검색어와 Qdrant Filter 조건을 작성**합니다.
    *(예: 쿼리="벨만포드 알고리즘", 필터=`{"skill": "python", "difficulty": "mid"}`)*
    2. Qdrant에서 필터 조건에 맞는 최상위 레퍼런스(Top-K)를 검색해옵니다.
    3. 검색된 문서와 유저 컨텍스트를 바탕으로 "왜 이 자료를 추천하는지(Reason)"를 짧게 생성합니다.

### 3️⃣ 커리큘럼 및 미션 생성 파이프라인 (Context-Aware Generation)

*단순 검색(RAG)이 아닌, 유저의 맥락(Context)을 이어나가는 행동 지침(Action Plan)을 생성합니다.*

- **Input:** 유저의 RDB 컨텍스트 + RAG로 찾아온 레퍼런스 문서
- **Process:** LLM 프롬프트에 유저의 최근 고민/학습 맥락을 주입하고, **'3단 레이아웃 UI'**에 들어갈 JSON을 한 번에 생성하도록 강제합니다.
- **Output (JSON):**
    
    ```
    {
      "reasoning": "최근 3주간 다익스트라 커밋이 집중되어, 음수 간선 처리를 위한 벨만-포드를 제안합니다.",
      "missions": [
        {"title": "다익스트라 복습 퀴즈", "type": "quiz", "estimated_time": "10분"},
        {"title": "수도코드 정리", "type": "action", "estimated_time": "40분"}
      ],
      "references": [ ... ] // RAG에서 찾아온 자료 매핑
    }
    ```
    

## 3. 기술 스택 및 주요 라이브러리 (AI 파트)

- **웹 프레임워크:** `FastAPI` (비동기 처리 최적화, 자동 API 문서화 Swagger 지원)
- **LLM 오케스트레이션:** `LangChain` (SelfQueryRetriever, LCEL 프롬프트 체인 활용)
- **데이터 검증 및 스키마:** `Pydantic` (FastAPI와 완벽 호환, LLM Structured Output 스키마 정의용)
- **Vector DB 클라이언트:** `qdrant-client`
- **사용 모델:** * 추출/분류/단순요약: `gpt-4o-mini` (가성비 및 속도 최고)
    - 커리큘럼/사고과정 생성: `gpt-4o` (복잡한 추론 필요 시 선택적 사용)

## 4. AI 서버 (FastAPI) 폴더 구조 제안

비즈니스 로직과 API 라우터를 명확히 분리하여 유지보수를 극대화하는 구조입니다.

```
fastapi-be/
├── app/
│   ├── main.py                 # FastAPI 애플리케이션 진입점
│   ├── api/
│   │   └── v1/
│   │       ├── extract.py      # POST /api/v1/extract (데이터 추출 엔드포인트)
│   │       ├── recommend.py    # POST /api/v1/recommend (RAG 기반 추천 엔드포인트)
│   │       └── curriculum.py   # POST /api/v1/curriculum (미션/커리큘럼 생성 엔드포인트)
│   ├── core/
│   │   ├── config.py           # 환경변수 (API Key, DB URI 등) 관리
│   │   └── qdrant.py           # Qdrant 연결 및 세팅 싱글톤 객체
│   ├── models/
│   │   ├── request.py          # BE에서 넘어오는 Request Body 스키마 (Pydantic)
│   │   └── response.py         # FE/BE로 반환할 Response 스키마 (Structured Output)
│   ├── services/
│   │   ├── extraction_svc.py   # Single LLM Pass 로직
│   │   ├── rag_svc.py          # LangChain SelfQueryRetriever 로직
│   │   └── generation_svc.py   # Context-Aware Generation 로직 (커리큘럼, 퀴즈)
│   └── prompts/
│       ├── system_prompts.py   # 직업, 페르소나 등이 포함된 프롬프트 템플릿 모음
│       └── query_prompts.py    # Self-Querying 용 프롬프트
├── requirements.txt            # 의존성 패키지 목록
└── .env                        # 환경 변수 (git 제외)
```

## 5. MVP 개발 마일스톤 (AI 팀 관점)

**Week 1: 추출 파이프라인 및 Qdrant 세팅**

- BE팀과 API Request/Response 스키마(JSON) 협의 및 정의
- `gpt-4o-mini` + `Pydantic`을 이용한 Single LLM Pass 데이터 추출 기능 완성
- Qdrant Cloud (Free Tier) 가입 및 초기 컬렉션(Collection) 세팅

**Week 2: 데이터 적재 및 Self-Querying RAG 구현**

- 크롤링 툴(Jina Reader 등)을 활용해 타겟 기술(예: React, Spring)의 핵심 공식 문서/블로그를 Qdrant에 임베딩 및 적재 (메타데이터 포함)
- LangChain의 `SelfQueryRetriever`를 적용하여, 유저 상태 기반 자동 메타데이터 필터링 검색 테스트

**Week 3: 커리큘럼 생성 고도화 및 연동 통합**

- 검색된 RAG 결과와 유저의 `user_activity` 로그를 조합하여 '3단 레이아웃' 양식에 맞는 최종 커리큘럼 JSON 생성 프롬프트 튜닝
- 퀴즈 생성 로직 결합
- BE, FE 팀과 최종 API 연동 및 디버깅 (Latency 최적화)

## 6. 고도화 및 추가 개발 방향 (Post-MVP)

서비스에 유저들의 활동 데이터가 충분히 적재된 이후, 추천의 신뢰도와 다양성을 폭발적으로 높이기 위한 고도화 기획입니다.

### 1️⃣ 사용자 유사도 기반 추천 (User-based Collaborative Filtering)

*쇼핑몰의 "이 상품을 구매한 다른 사용자들이 많이 본 상품"과 같은 소셜 증명(Social Proof) 기반 추천입니다.*

- **기능:** "OO님과 비슷한 프론트엔드 스택(React, TS)을 가진 개발자들은 다음 단계로 **[Zustand를 활용한 전역 상태 관리]**를 많이 학습했습니다."
- **구현 방향:**
    - 유저들의 `learning_states`(스킬 점수 맵) 데이터를 다차원 벡터로 변환하여 Qdrant에 **유저 벡터(User Vector)** 로 저장합니다.
    - 추천 쿼리가 들어올 때, 해당 유저의 벡터와 가장 거리(Cosine Similarity)가 가까운 '유사 유저 그룹(Nearest Neighbors)'을 검색합니다.
    - 이 유사 집단이 최근에 높은 성취도를 보인 커리큘럼이나 레퍼런스를 추출하여 LLM 프롬프트에 힌트로 주입합니다.

### 2️⃣ 콘텐츠 유사도 기반 추천 (Content-based Filtering)

*쇼핑몰의 "현재 보고 있는 상품과 비슷한 다른 상품"과 같은 연관 자료 확장 추천입니다.*

- **기능:** 추천받은 문서 하단에 "지금 보고 계신 **[벨만-포드 위키독스]**와 가장 유사한 난이도의 심화 블로그 글 3개" 제공
- **구현 방향:**
    - 유저가 텍스트 쿼리를 날리는 것이 아니라, 특정 문서(Reference) 자체의 **문서 벡터(Document Vector)** 를 기준점(Query)으로 삼아 Qdrant에 벡터 유사도 검색(Similarity Search)을 바로 요청합니다.
    - LLM을 거칠 필요가 없어 응답 속도가 매우 빠르며(수십 ms), 유저가 학습 자료를 꼬리에 꼬리를 물고 탐색할 수 있도록 돕습니다.

### 3️⃣ 지식 그래프 (Knowledge Graph) 도입

- 단순한 벡터 유사도를 넘어, 기술 간의 선후 관계(예: 'React'를 배우기 위해선 'JS ES6'가 선행되어야 함)를 그래프 DB 모델(Neo4j 등)로 관리하여 더욱 체계적이고 절대 오류가 없는 로드맵을 제공합니다.
# AI 추천·퀴즈 서비스 개선 작업 로그

> 작성일: 2026-03-17
> 작업자: AI 파트
> 관련 이슈: S14P21A506-121
> 관련 파일: `fastapi-be/app/services/recommendation_svc.py`, `quiz_svc.py`, `core/model_router.py`, `tests/`

---

## 1. 작업 배경

이전 성능 테스트(`AI_성능테스트_및_아키텍처논의.md`)에서 도출된 하기 Action Items를 이번 세션에서 구현하고 검증함.

| 우선순위 | 항목 | 이번 작업 결과 |
|---------|------|--------------|
| P2 | quiz_svc — 질문+루브릭 단일 LLM 호출로 병합 | ✅ 완료 |
| P3 | 추천 reason 프롬프트 — target_positions 언급 강화 | 미착수 (다음 세션) |
| - | 추천 결과 난이도 다양성 확보 | ✅ 신규 추가 |
| - | mock 데이터 커버리지 확장 | ✅ 신규 추가 |

---

## 2. 변경 사항 상세

### 2-1. 추천 난이도 균형 선택 (`recommendation_svc.py`)

**문제**: `_select_top_k`가 ranked list 상위 K개를 단순 절사하여, 사용자 스킬 분포에 따라 추천 5개가 전부 같은 난이도로 편중됨. 학습 다양성이 없어 사용자 선택의 폭이 좁아짐.

**변경**: `_select_with_difficulty_balance` 함수 신규 추가, `run_recommendation`에서 호출.

**로직**:
```
_DIFFICULTY_RATIO = {low: 1, mid: 2, high: 1}

k=5 기준 할당:
  low  = ceil(5 × 1/4) = 2
  mid  = floor(5 × 2/4) = 2
  high = 1
  (extra slot → low: 약점 우선 보완 원칙)
```

**동작 방식**:
1. ranked pool을 low/mid/high 버킷으로 분류
2. 각 버킷에서 quota만큼 선택 (rank 순서 유지)
3. 버킷 부족 시 surplus에서 weak-skill 우선 채움
4. 최종 선택된 아이템을 `_rank_by_learning_priority`와 동일한 키로 재정렬 (weak-first 보장)

**pool 부족 시 동작**: 특정 난이도 후보가 pool에 없으면 다른 난이도로 채움. 전부 high인 경우 high=5, 전부 low인 경우 low=5로 graceful fallback.

**테스트 결과** (`test_recommendation_quality.py`):
```
all_low   → low=5  mid=0  high=0  (pool 부족 — fallback 정상 동작)  ALL PASS
mixed     → low=2  mid=2  high=1  ✓ 목표 비율                       ALL PASS
all_high  → low=0  mid=0  high=5  (pool 부족 — fallback 정상 동작)  ALL PASS
```

**관련 상수 추가**:
```python
# Spring Boot 상대 rank 산정 기준 (30/40/30 percentile):
#   bottom 30% → low,  middle 40% → mid,  top 30% → high
_DIFFICULTY_RATIO = {low: 1, mid: 2, high: 1}
```

> Spring Boot가 `relative_rank`를 산정할 때 **전체 유저 대비 백분위**를 기준으로 30/40/30 분할을 사용해야 AI 서비스의 난이도 비율이 의도대로 동작함. AI 서비스는 입력값을 그대로 신뢰하므로 Spring Boot 측에서 이 기준을 준수해야 함.

---

### 2-2. mock 데이터 확충 (`tests/mock_recommendation_data.json`)

**문제**: 기존 10개 문서가 Spring/JWT/Python 계열에만 집중되어, Java/C++ 강점 사용자에게 high 난이도 추천이 불가능했음. balance 로직의 high 버킷이 항상 비어 검증이 불가능했음.

**추가 문서 6개**:

| doc_id | title | skill_tags | 추가 이유 |
|--------|-------|-----------|----------|
| `ref_java_design_pattern_001` | Effective Java — 객체 생성과 설계 패턴 | Java, Design Pattern, OOP | Java high rank 커버 |
| `ref_java_jvm_001` | JVM 내부 구조와 GC 튜닝 가이드 | Java, JVM, Performance | Java 심화 |
| `ref_cpp_stl_001` | Modern C++ STL 완벽 가이드 (C++17/20) | C++, STL, Algorithm | C++ high rank 커버 |
| `ref_nestjs_typescript_001` | NestJS 공식 문서 — 모듈 & 의존성 주입 | NestJS, TypeScript, Node.js | NestJS mid rank 커버 |
| `ref_docker_cicd_001` | Docker + GitHub Actions CI/CD 파이프라인 | Docker, CI/CD, GitHub Actions | Docker mid rank 커버 |
| `ref_typescript_handbook_001` | TypeScript 핸드북 — 타입 시스템 기초 | TypeScript, JavaScript, Frontend | TypeScript low rank 커버 |

**확충 후 실 데이터(zhy2on) 추천 결과**:
```
[low ] TypeScript 핸드북 — 타입 시스템 기초      ← TypeScript low rank
[low ] Vue 3 Composable 패턴 가이드              ← Vue low rank
[mid ] NestJS 공식 문서 — 모듈 & 의존성 주입    ← NestJS mid rank
[mid ] JWT Best Current Practices               ← 관련 학습
[high] JVM 내부 구조와 GC 튜닝 가이드           ← Java high rank ✓
```

---

### 2-3. E2E 테스트 rank 파생 로직 수정 (`tests/test_e2e_profile_to_quiz.py`)

**문제**: `_derive_learning_states`가 `skill_frequency`를 내림차순 정렬 후 index 기준으로 rank를 배정하여 **역방향** 배정이 발생함.
- Java freq=38 → rank=low (잘못됨)
- FastAPI freq=1 → rank=high (잘못됨)

**수정**: percentile 기반 30/40/30 분할로 전면 재작성.

```python
# 수정 전 (잘못된 로직)
sorted_skills = sorted(skill_frequency.items(), key=lambda x: x[1])  # 오름차순
# index 0 = 빈도 낮음 = low 배정 → 정렬 후 상위 6개만 취하면 전부 강한 스킬

# 수정 후 (30/40/30 percentile 기준)
def _percentile(freq):
    below = sum(1 for f in all_freqs if f < freq)
    return int(below / n * 100)

def _rank(pct):
    if pct > 70:  return RelativeRank.high   # 상위 30%: 자주 사용 = 강점
    elif pct > 30: return RelativeRank.mid
    else:          return RelativeRank.low   # 하위 30%: 거의 안 씀 = 약점
```

또한 LearningState 8개를 low:mid:high = 1:2:1 비율로 구간별 균형 선택하여 추천 balance 로직과 일관성 유지.

---

### 2-4. 퀴즈 생성 단일 LLM 호출 병합 (`quiz_svc.py`)

**문제**: `generate_questions()` → `generate_rubric()` 2회 순차 호출로 구조적으로 병렬화 불가. avg 8.29s로 목표(8s) 경계.

**gpt-4o-mini 실험 결과 (기각)**:

| 모델 | avg | 비고 |
|---|---|---|
| gpt-4o | 8.29s | 기존 |
| gpt-4o-mini | 12.19s | **오히려 느림** |

gpt-4o-mini가 느린 원인: GMS 프록시 환경에서 `with_structured_output`의 중첩 스키마(`per_question_rubrics: list[_PerQuestionRubric]`) 처리 시 토큰 생성이 더 오래 걸림. 모델 자체 성능 차이보다 프록시 라우팅 차이가 지배적으로 작용.

**실제 해결책: combined 단일 호출**

`_CombinedOutput` 스키마를 신규 추가하여 questions + rubric을 한 번의 LLM 호출로 생성.

```python
class _CombinedOutput(BaseModel):
    questions: list[_SingleQuestion]
    full_score_criteria: list[str]
    partial_score_criteria: list[str]
    per_question_rubrics: list[_PerQuestionRubric]
```

**신규 함수 구조**:
```
generate_questions_and_rubric()   ← run_quiz에서 호출
  ├─ primary  : _generate_combined_with_llm()  (단일 호출)
  └─ fallback : generate_questions() + generate_rubric()  (기존 2-call)
```

**측정 결과** (3회 평균):

| 방식 | avg | 목표 |
|---|---|---|
| 2-call 순차 (기존) | 8.29s | ≤8s |
| combined 단일 호출 | **7.12s** | ≤8s ✓ |
| 단축 | **1.17s (14%)** | - |

**품질 테스트**: 6/6 ALL PASS (combined 경로로 동일 품질 유지)

---

## 3. 최종 성능 측정 결과 (2026-03-17)

실 데이터: GitHub ID `zhy2on` (108개 레포지토리, Velog 20개 게시글)

| 항목 | 측정값 | 목표 | 판정 |
|------|--------|------|------|
| profile_total | 59.05s | ≤60s | ✓ |
| recommendation | 3.60~4.27s | ≤5s | ✓ |
| quiz (combined) | 7.12s avg | ≤8s | ✓ |
| **hot path (rec + quiz)** | **~11s** | ≤13s | ✓ |

> **참고**: Qdrant `fastembed` 미설치 상태(fallback 동작). Qdrant 실 연동 후 재측정 필요.

---

## 4. 실 데이터 분석 결과 (zhy2on 프로필)

```
headline  : 다양한 기술 스택을 활용한 웹 애플리케이션 개발 전문가
강점 스킬  : Java(96%), C++(92%), JavaScript(88%)
중간 스킬  : NestJS, Docker, C, SCSS
약점 스킬  : TypeScript(0%), Vue(0%)  ← 거의 사용 이력 없음
```

추천 결과: TypeScript/Vue 입문 → NestJS/JWT 중급 → JVM 고급 순으로 구성.

---

## 5. 수정된 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `app/services/recommendation_svc.py` | `_select_with_difficulty_balance` 추가, `_DIFFICULTY_RATIO` 상수, `run_recommendation` 호출부 변경 |
| `app/services/quiz_svc.py` | `_CombinedOutput` 스키마, `_COMBINED_SYSTEM_PROMPT`, `_COMBINED_USER_TEMPLATE`, `generate_questions_and_rubric`, `_generate_combined_with_llm` 추가, `run_quiz` 호출부 변경, docstring 업데이트 |
| `app/core/model_router.py` | 변경 없음 (gpt-4o-mini 실험 후 원복) |
| `tests/mock_recommendation_data.json` | 문서 10개 → 16개 (Java, C++, NestJS, Docker, TypeScript 계열 추가) |
| `tests/test_e2e_profile_to_quiz.py` | `_derive_learning_states` 전면 재작성 (30/40/30 percentile 기준) |
| `tests/fixtures/zhy2on_profile.json` | 신규 생성 — zhy2on 실 분석 결과 캐시 |

---

## 6. 다음 세션 Action Items

| 우선순위 | 항목 | 비고 |
|---------|------|------|
| P1 | `pip install fastembed` 후 Qdrant 재테스트 | 현재 수치는 fallback 기준 |
| P1 | Spring Boot — GitHub 연동 시점에 profile 분석 trigger | FastAPI 코드 변경 불필요 |
| P2 | 추천 item reason 구체성 개선 | 현재 "Python 관련 이해도를 높이는 데..." 수준으로 일반적 |
| P3 | 추천 overall reason에 target_position 명시 강화 | 프롬프트 수정 |

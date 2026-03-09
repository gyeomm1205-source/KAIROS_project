## **1. 문서 목적**

- 본 문서는 `document/02-table-spec.md`를 시각적으로 보완하는 논리 ERD 초안이다.
- `document/01-fun-spec.csv`의 MVP 요구사항을 만족하는 엔티티, 관계, 핵심 속성만 포함한다.
- 물리 컬럼 타입, 인덱스, 제약조건 확정 이전 단계의 설계 문서로 사용한다.

## **2. 작성 원칙**

- `UserActivity`를 서비스 전반의 단일 원천 활동 로그로 본다.
- 스킬 상태는 `LearningState` 1:N 구조를 정식 저장소로 본다.
- 외부 연동은 GitHub·Velog 필수 연동과 Google Calendar 연동으로 구분한다.
- 페르소나는 템플릿 기반 `UserPersona`만 사용한다.
- 캘린더 동기화는 `CalendarIntegration`, `CalendarSyncJob`, `CalendarEventMapping`으로 분리한다.
- 프롬프트 대화는 일정 노드별 `PromptConversation + PromptMessage`로 저장한다.

## **3. 전체 ERD**

```mermaid
erDiagram
    User ||--|| UserProfile : has
    UserPersona ||--o{ UserProfile : selected_by
    UserPersona ||--o{ OnboardingSurvey : selected_in
    User ||--o{ ExternalAccountConnection : connects
    User ||--o{ OnboardingSurvey : submits
    User ||--o{ UserActivity : records
    User ||--o{ LearningState : tracks
    User ||--o{ LearningStateRecomputeJob : recalculates
    User ||--o{ AnalysisJob : runs
    AnalysisJob ||--|| AnalysisResult : produces
    AnalysisResult ||--o{ AnalysisFeedback : receives
    AnalysisResult ||--o{ Recommendation : seeds
    User ||--o{ Recommendation : receives
    Recommendation ||--o{ RecommendationItem : contains
    Recommendation ||--|| RecommendationReasonSnapshot : explains
    Recommendation ||--o{ ReferenceRecommendation : suggests
    ReferenceRecommendation ||--|| ReferenceValidationReport : validates
    Recommendation ||--o| Curriculum : materializes
    User ||--o{ Curriculum : owns
    User ||--o{ LearningTrack : configures
    Curriculum ||--o{ CurriculumNode : expands
    LearningTrack ||--o{ CurriculumNode : groups
    CurriculumNode ||--o{ CurriculumNodeRelation : source_of
    CurriculumNodeRelation }o--|| CurriculumNode : targets
    CurriculumNode o|--o{ Quiz : attaches
    Quiz ||--o{ QuizAttempt : records
    User ||--o{ QuizAttempt : submits
    User ||--|| CalendarIntegration : owns
    User ||--o{ CalendarSyncJob : syncs
    CalendarIntegration ||--o{ CalendarSyncJob : runs
    CalendarIntegration ||--o{ CalendarEventMapping : manages
    CurriculumNode ||--o{ CalendarEventMapping : maps
    CurriculumNode ||--o| PromptConversation : opens
    PromptConversation ||--o{ PromptMessage : contains
```

## **4. 계정 · 프로필 · 활동 ERD**

```mermaid
erDiagram
    User {
        uuid id PK
        string google_sub UK
        string account_stage
        string role
        datetime created_at
        datetime updated_at
    }

    UserProfile {
        uuid user_id PK
        string nickname
        string scope_filter
        string desired_position
        json current_tech_stacks
        uuid persona_id FK
        datetime updated_at
    }

    UserPersona {
        uuid id PK
        string code UK
        string name
        text prompt_template
    }

    ExternalAccountConnection {
        uuid id PK
        uuid user_id FK
        string provider
        string account_identifier
        string status
        datetime connected_at
        datetime token_expires_at
    }

    OnboardingSurvey {
        uuid id PK
        uuid user_id FK
        json selected_scopes
        string current_job
        json tech_stacks
        string desired_position
        uuid persona_id FK
        datetime submitted_at
    }

    UserActivity {
        uuid id PK
        uuid user_id FK
        string source
        string category
        json tech_stacks
        text summary
        datetime activity_date
        boolean is_excluded
    }

    LearningState {
        uuid id PK
        uuid user_id FK
        string tech_stack
        decimal score
        string level
        datetime updated_at
    }

    LearningStateRecomputeJob {
        uuid id PK
        uuid user_id FK
        string policy_version
        datetime executed_at
        string status
    }

    User ||--|| UserProfile : has
    UserPersona ||--o{ UserProfile : selected_by
    UserPersona ||--o{ OnboardingSurvey : selected_in
    User ||--o{ ExternalAccountConnection : connects
    User ||--o{ OnboardingSurvey : submits
    User ||--o{ UserActivity : records
    User ||--o{ LearningState : tracks
    User ||--o{ LearningStateRecomputeJob : recalculates
```

## **5. 분석 · 추천 · 학습 ERD**

```mermaid
erDiagram
    User {
        uuid id PK
        string role
    }

    AnalysisJob {
        uuid id PK
        uuid user_id FK
        string status
        int progress_percent
        datetime requested_at
        datetime completed_at
    }

    AnalysisResult {
        uuid id PK
        uuid analysis_job_id FK
        json summary_payload
        json detected_stacks
        json recommended_positions
        datetime created_at
    }

    AnalysisFeedback {
        uuid id PK
        uuid analysis_result_id FK
        text correction_note
        json override_payload
        datetime submitted_at
    }

    Recommendation {
        uuid id PK
        uuid user_id FK
        uuid analysis_result_id FK
        string recommendation_type
        string status
        datetime created_at
    }

    RecommendationItem {
        uuid id PK
        uuid recommendation_id FK
        string item_type
        string title
        string status
        int display_order
    }

    RecommendationReasonSnapshot {
        uuid id PK
        uuid recommendation_id FK
        json reason_payload
        json reference_links
        datetime created_at
    }

    ReferenceRecommendation {
        uuid id PK
        uuid recommendation_id FK
        string url
        string topic
        text reason_summary
    }

    ReferenceValidationReport {
        uuid id PK
        uuid reference_recommendation_id FK
        boolean is_safe
        boolean is_recent
        text validation_summary
    }

    Curriculum {
        uuid id PK
        uuid user_id FK
        uuid recommendation_id FK
        string title
        string status
        datetime created_at
    }

    CurriculumNode {
        uuid id PK
        uuid curriculum_id FK
        uuid learning_track_id FK
        uuid parent_node_id FK
        date scheduled_date
        string title
        boolean is_completed
    }

    CurriculumNodeRelation {
        uuid id PK
        uuid source_node_id FK
        uuid target_node_id FK
        string relation_type
        datetime created_at
    }

    Quiz {
        uuid id PK
        uuid curriculum_node_id FK
        string quiz_type
        json question_set
        datetime created_at
    }

    QuizAttempt {
        uuid id PK
        uuid quiz_id FK
        uuid user_id FK
        string status
        decimal score
        decimal learning_state_delta
        datetime submitted_at
    }

    User ||--o{ AnalysisJob : runs
    AnalysisJob ||--|| AnalysisResult : produces
    AnalysisResult ||--o{ AnalysisFeedback : receives
    AnalysisResult ||--o{ Recommendation : seeds
    User ||--o{ Recommendation : receives
    Recommendation ||--o{ RecommendationItem : contains
    Recommendation ||--|| RecommendationReasonSnapshot : explains
    Recommendation ||--o{ ReferenceRecommendation : suggests
    ReferenceRecommendation ||--|| ReferenceValidationReport : validates
    Recommendation ||--o| Curriculum : materializes
    User ||--o{ Curriculum : owns
    Curriculum ||--o{ CurriculumNode : expands
    CurriculumNode ||--o{ CurriculumNodeRelation : source_of
    CurriculumNodeRelation }o--|| CurriculumNode : targets
    CurriculumNode o|--o{ Quiz : attaches
    Quiz ||--o{ QuizAttempt : records
    User ||--o{ QuizAttempt : submits
```

## **6. 캘린더 · 프롬프트 ERD**

```mermaid
erDiagram
    User {
        uuid id PK
        string role
    }

    LearningTrack {
        uuid id PK
        uuid user_id FK
        string name
        string color
        int display_order
        boolean is_system
    }

    CalendarIntegration {
        uuid id PK
        uuid user_id FK
        string provider
        string status
        datetime last_synced_at
    }

    CalendarSyncJob {
        uuid id PK
        uuid user_id FK
        uuid calendar_integration_id FK
        string sync_type
        string status
        datetime executed_at
    }

    CurriculumNode {
        uuid id PK
        uuid curriculum_id FK
        uuid learning_track_id FK
        string title
        date scheduled_date
    }

    CalendarEventMapping {
        uuid id PK
        uuid calendar_integration_id FK
        uuid curriculum_node_id FK
        string external_event_id
        string external_calendar_id
        string mapping_status
    }

    PromptConversation {
        uuid id PK
        uuid curriculum_node_id FK
        datetime created_at
        datetime updated_at
    }

    PromptMessage {
        uuid id PK
        uuid prompt_conversation_id FK
        string role
        text content
        datetime created_at
    }

    User ||--o{ LearningTrack : configures
    User ||--|| CalendarIntegration : owns
    User ||--o{ CalendarSyncJob : syncs
    LearningTrack ||--o{ CurriculumNode : groups
    CalendarIntegration ||--o{ CalendarSyncJob : runs
    CalendarIntegration ||--o{ CalendarEventMapping : manages
    CurriculumNode ||--o{ CalendarEventMapping : maps
    CurriculumNode ||--o| PromptConversation : opens
    PromptConversation ||--o{ PromptMessage : contains
```

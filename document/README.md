# Document Index

이 폴더는 기능 명세를 기준으로 데이터/ERD/API 설계를 단계적으로 정리한 문서 묶음이다.

## 읽는 순서

1. `01-fun-spec.csv`
   - 현재 MVP 기준 기능 명세의 최종 기준 문서.
   - 문서 폴더 내 다른 설계 문서는 이 파일과 정합해야 한다.
2. `02-table-spec.md`
   - 기능 명세를 엔티티/관계 관점으로 해석한 논리 데이터 모델 설명.
3. `03-ERD.md`
   - 테이블 명세를 Mermaid ERD로 시각화한 문서.
4. `04-api-spec.csv`
   - 화면/플로우 기준으로 정리한 API 계약 초안.

## 범위 원칙

- 기준 문서는 항상 `01-fun-spec.csv`다.
- MVP 제외 기능은 `02-table-spec.md`, `03-ERD.md`, `04-api-spec.csv`에서 제거하거나 별도 확장 항목으로만 언급한다.
- Google Calendar 외 Notion/CSV import, 블로그 어시스턴트, custom persona markdown은 현재 문서 범위에서 제외한다.

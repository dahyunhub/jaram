# 자람 (jaram)

유치원 교사 업무 도구 — 메모 기록, AI 보육일지/개인평가 생성 서비스.

설계 산출물은 [`_bmad-output/planning-artifacts/`](_bmad-output/planning-artifacts/)
(PRD · 아키텍처 · 에픽/스토리)와 [`docs/`](docs/)를 정본으로 따른다.

## 저장소 구조

```
jaram/
├── backend/        # Spring Boot 4.0.7 + Java 25 (Gradle) REST API
│   └── src/main/java/com/jaram/   # package-by-feature: auth, classroom, child,
│                                  #   memo, journal, report, ai, common, config
├── frontend/       # Vue 3 + Vite SPA
├── docs/           # 통합 기획서
└── _bmad-output/   # PRD · 아키텍처 · 에픽/스토리 (BMad 산출물)
```

## 기술 스택

| 영역      | 선택                                   |
|-----------|----------------------------------------|
| 백엔드    | Java 25 LTS · Spring Boot 4.0.7 · Gradle 9.5 |
| DB        | MySQL 8.x · Spring Data JPA · Flyway (스키마 정본) |
| 인증      | Spring Security · JWT · BCrypt          |
| AI        | 외부 LLM API (Claude/OpenAI) · RestClient |
| 프론트    | Vue 3 (Composition API) · Vite 8       |
| 인프라    | Docker + docker-compose (예정)          |

## 개발 실행

### 백엔드

```bash
cd backend
cp .env.example .env          # 환경변수 채우기
./gradlew build               # 빌드 + 테스트
./gradlew bootRun             # 실행 (MySQL 필요)
```

- 기동에는 MySQL 이 필요하다(`db/migration` 의 Flyway 마이그레이션 적용). 컨테이너 구성은 이후 스토리(1.5)에서 추가된다.
- 헬스체크: `GET /actuator/health`

### 프론트엔드

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5173 (→ /api 는 :8080 으로 프록시)
npm run build                 # 프로덕션 빌드
```

## 현재 상태

Story 1.1 (프로젝트 골격) 완료 — 빌드 통과, package-by-feature 골격 마련.
다음: Flyway V1 스키마 · 인증(JWT) · 메모/일지/평가 기능 (에픽 1~4 참고).

---
title: 온도 — 디자인 ↔ 백엔드 API 정합 가이드 (Design ↔ API Alignment)
status: active
created: 2026-06-16
sources:
  - Claude Design 핸드오프 번들 (온도.html + screens-*.jsx + data.jsx + tokens.css)
  - docs/specs/api-spec.md
  - docs/specs/data-model-spec.md
purpose: 프론트(Vue, Claude Design 구현물)가 소비하는 백엔드 REST 계약과 디자인 목업의 데이터/화면을 1:1로 대조한 정합 문서. 프론트 연동 시 매핑 기준.
scope: 백엔드는 enum 값(예 SOCIAL)을 그대로 반환하고, 한글 라벨·표시 포맷은 프론트가 매핑한다.
---

# 디자인 ↔ 백엔드 API 정합 가이드

클로드 디자인 목업(정적 React/JSX, 실제 API 호출 없음 — `data.jsx`가 기대 데이터 형태)을 백엔드 API와 대조한 결과. **대부분 정합**하며, 발견된 단일 갭(성별)은 V2로 반영 완료.

## 1. 화면 → 엔드포인트 매핑

| 디자인 화면 | 백엔드 엔드포인트 | 비고 |
|------------|------------------|------|
| 로그인 (auth) | `POST /api/v1/auth/login` | 이메일/비번 → JWT |
| 반 선택 (account) | `GET /api/v1/classrooms` | year·아이수 포함 |
| 아이 명단·등록 (register) | `GET·POST /api/v1/classrooms/{id}/children`, `PUT·DELETE /api/v1/children/{id}` | 성별 토글 포함 |
| 메모 입력 (memo) | `POST /api/v1/memos` | 3항목(놀이/상호작용/태도) |
| 홈 피드(오늘 메모) | (타임라인/메모 조회 조합) | `todayMemos` = 당일 메모 |
| 일지 초안·재분석 (journal) | `POST /api/v1/journals/analyze`, `GET·PUT /api/v1/journals`, `POST /journals/{id}/analyze` | 5영역 blocks |
| 아이 타임라인 (timeline) | `GET /api/v1/children/{id}/timeline` | area 필터 |
| 개인평가 (journal/extra) | `POST·GET /api/v1/children/{id}/reports`, `GET /api/v1/reports/{id}` | 수동/자동 누적 |
| 마이 (misc/system) | (교사 정보 — 로그인 응답의 teacher) | 별도 엔드포인트 없음(MVP) |

## 2. enum / 코드 매핑 (프론트가 변환)

### 누리과정 영역
| 디자인 코드 | 백엔드 enum | 한글 |
|---|---|---|
| `social` | `SOCIAL` | 사회관계 |
| `body` | `PHYSICAL_HEALTH` | 신체운동·건강 |
| `art` | `ART` | 예술경험 |
| `nature` | `NATURE` | 자연탐구 |
| `comm` | `COMMUNICATION` | 의사소통 |
| `uncat` | `null` (타임라인 필터는 `UNCLASSIFIED`) | 미분류 |

### 기타 enum
| 디자인 | 백엔드 |
|---|---|
| 성별 `남` / `여` | `gender` = `MALE` / `FEMALE` |
| 평가 종류 `수동` / `자동` | `reportType` = `MANUAL` / `MONTHLY` |
| 일지 상태 (초안/확정) | `status` = `DRAFT` / `CONFIRMED` |

## 3. 데이터 형태 대조 (`data.jsx` ↔ API)

| 디자인 필드 | 백엔드 응답 | 정합 |
|---|---|---|
| child: `name`, `birth`, `sex`, `memos`(수) | `name`, `birthDate`, `gender`, (memo count 별도) | ✅ (성별 V2 반영) |
| class: `name`, `count`, `year`("2026학년도"), `tag`("올해/작년") | `name`, `childCount`, `year`(int), `startDate` | ✅ `year` 문자열·`tag`는 프론트 파생 |
| memo: `놀이`/`의사소통·상호작용`/`수업태도`, `area`, `time` | `playActivity`/`interaction`/`attitude`, `curriculumArea`, `createdAt` | ✅ (라벨 매핑) |
| journalDraft: 5영역 `blocks[{area, txt}]` | journal `content`(5영역 구조) | ✅ |
| childEvals: `title`,`period`,`kind`,`areas` | child_report `reportType`,`period_start/end` | ✅ |
| timeline: `date`,`time`,`area`,`txt` | timeline `date`,`createdAt`,`curriculumArea`,`content` | ✅ |
| teacher: `teacher`(이름), `teacherEmail` | 로그인 응답 `teacher{ id,email,name }` | ✅ |

## 4. 갭 & 처리

| 갭 | 상태 |
|---|---|
| **성별(sex)** — 디자인 명단/등록에 있으나 백엔드에 없었음 | ✅ **해결** — Flyway V2 `child.gender`(MALE/FEMALE) + 엔티티/DTO/검증/테스트 추가 |
| `memos`(아이별 메모 수) — 명단 화면 표시 | 명단 응답엔 미포함. 필요 시 child 응답에 count 추가 검토(현재는 타임라인 조회로 대체 가능) — **후속 검토** |
| 메모 `content`(자유 입력) — 디자인은 3항목 위주 | 백엔드는 `content` nullable + 3항목 중 1개 이상. 프론트가 3항목만 보내도 OK |
| 일지 `content` 내부 JSON 스키마 | api-spec §9 미확정 — AI 연동 명세에서 확정 예정(Epic 3) |

## 5. 프론트 연동 노트

- 모든 enum은 백엔드가 **코드값 그대로** 반환 → 프론트가 한글 라벨·색(tokens.css의 area별 색)으로 매핑.
- 날짜: API는 `YYYY-MM-DD`/ISO-8601 UTC. 디자인의 "6월 14일"·"2026학년도"·"올해/작년"은 프론트 표시 변환.
- 인증: `Authorization: Bearer <JWT>`. 로그인·`/actuator/health` 외 전부 필요.
- 비식별화: 응답의 `name`(실명)은 소유 교사에게만. 외부 AI 전송 시에만 `[[CHILD_n]]` 치환(NFR-1) — 프론트는 그대로 표시.

> 프론트엔드(Vue) 구현은 사용자 트랙(Claude Design). 본 문서는 백엔드 계약 정합 기준만 제공.

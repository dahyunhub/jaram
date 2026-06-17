---
title: 자람 — AI 연동 명세 (AI Integration Spec)
status: draft
created: 2026-06-16
sources:
  - _bmad-output/planning-artifacts/architecture.md (AI Integration, TX 경계, 네이밍/구조)
  - _bmad-output/planning-artifacts/prds/prd-jaram-2026-06-15/addendum.md (§C 프롬프트 강제 요구)
  - docs/specs/api-spec.md (§5 일지, §6 평가, §9 미확정)
  - docs/specs/data-model-spec.md (daily_journal.content, child_report.content)
  - docs/specs/error-code-catalog.md (AI_* 코드)
provider: OpenAI API (사용자 확정 2026-06-16). 벤더는 AiClient 뒤로 추상화 — Claude 등으로 교체 가능.
purpose: Epic 3/4 구현 전 선행 명세. api-spec §9의 미확정(일지 content JSON 스키마·AI 응답 파싱 타입)을 확정한다.
---

# AI 연동 명세 — 자람

외부 LLM(**OpenAI API**) 연동의 구현 계약. **일지/평가 content의 JSON 스키마**를 확정해 Epic 3/4 진입 블로커를 해소한다.

## 0. 원칙 (아키텍처 정본)

- **RestClient raw HTTP**: OpenAI Java SDK·Spring AI 미사용. Spring 내장 `RestClient`로 OpenAI API 직접 호출.
- **벤더 추상화**: `AiClient` 인터페이스. 구현 `OpenAiClient`(인프라)는 `AiClient` 뒤에만. Deidentifier·OutputValidator·AnalysisGuard는 `OpenAiClient`를 **모름**(의존성 방향 고정). 벤더 교체 시 구현만 추가.
- **3경로**: ① 동기 on-demand(일지 FR-3 / 수동평가 FR-8), ② 야간 묶음의 영역분류(FR-2)는 **일지 분석 패스에 통합**(별도 잡 없음), ③ 월말 자동(FR-9) `@Scheduled` 순차.
- **TX 경계**: 오케스트레이션(JournalService/ReportService) no-TX → AI 호출 TX 밖 → 결과 저장 PersistService(REQUIRES_NEW).
- **비용/자산**: 메모 단위 호출 금지, 묶음 1회. 사용자당 동시 분석 1건(AnalysisGuard). AI 실패해도 원본 메모 보존(FR-4).

## 1. LLM API 호출 계약 (OpenAI Chat Completions)

> 모델·키는 env 주입(`AI_API_KEY`, `AI_MODEL`). 기본 모델은 **운영 시점의 최신 OpenAI 모델 ID로 확인·설정**(예: `gpt-4.1` / `gpt-4o`, 비용 우선 시 `gpt-4o-mini` 류). 하드코딩하지 말고 env로 주입.

**Endpoint:** `POST https://api.openai.com/v1/chat/completions`
**Headers:** `Authorization: Bearer ${AI_API_KEY}`, `content-type: application/json`

**Request(body) 핵심 필드:**
```json
{
  "model": "${AI_MODEL}",
  "max_tokens": 4096,
  "messages": [
    { "role": "system", "content": "<프롬프트 템플릿: 누리 5영역 규칙·출력 JSON 형식 강제>" },
    { "role": "user",   "content": "<비식별화된 메모 묶음 + 지시>" }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": { "name": "journal", "strict": true, "schema": { /* §3/§4 스키마 */ } }
  }
}
```

- **structured outputs(`response_format.json_schema`, `strict: true`)**: 응답을 JSON 스키마로 강제 → 형태 유효성 보장(파싱 실패류 제거). OpenAI strict 모드 요건과 §3/§4 스키마가 일치: **모든 property가 `required`**, 모든 객체 `additionalProperties: false`. **내용 완전성**(5영역 누락 등)은 `OutputValidator`가 별도 검사.
- **temperature**: 기본(미지정) 또는 낮게(0.3~0.7). 일관성·비용 우선. 추론형(o-시리즈) 모델 사용 시 `temperature` 미지원 — 채팅형(gpt-4.x/4o) 권장.
- **prompt caching**: OpenAI는 system 등 동일 프리픽스를 **자동 캐싱**(별도 파라미터 없음). 템플릿(고정)을 system 앞단에, 변동부(메모)를 user에 두면 야간/월말 묶음에서 자동 절감(NFR-4).
- **max_tokens**: 일지 ~4096, 평가 ~2048. 스트리밍 미사용(동기 단발, ~20s 하드 타임아웃 내).

**Response 파싱:** `choices[0].message.content`(strict면 유효 JSON 문자열), `choices[0].finish_reason`, `usage`. 안전상 거절 시 `choices[0].message.refusal`(non-null)이 올 수 있음 → AI 실패 처리.

**상태/에러 매핑(error-code-catalog):**
| 상황 | 처리 |
|------|------|
| 연결 실패 · 5xx · 429(rate limit) | **재시도**(지수백오프, 1~2회). 소진 시 `AI_ANALYSIS_FAILED`(502) |
| 서버 하드 타임아웃(~20s, 프록시보다 짧게) 초과 | **재시도 안 함** → `AI_TIMEOUT`(504) |
| 200 + `message.refusal` non-null (또는 content 비어있음) | 분석 불가 → `AI_ANALYSIS_FAILED` (메모 보존 고지) |
| OutputValidator 실패(1회 재요청 후에도) | `AI_OUTPUT_INVALID`(502) |
| 복원 실패([[CHILD_n]] 잔존) | `DEIDENTIFICATION_RESTORE_FAILED`(500) |

> 모든 AI 계열 에러 메시지는 "작성하신 메모는 그대로 저장돼 있어요" 포함(FR-4).

## 2. AiClient 인터페이스 (com.jaram.ai)

```java
public interface AiClient {
    /** 프롬프트를 보내고 모델 원문 텍스트(structured면 JSON 문자열)를 반환.
     *  타임아웃/재시도/HTTP 매핑은 구현체 책임. 실패 시 AiAnalysisException. */
    String complete(AiRequest request);
}

public record AiRequest(String systemPrompt, String userPrompt, String jsonSchema, int maxTokens) {}
```

- `OpenAiClient`: RestClient + `AiClientConfig`(타임아웃·모델·키 properties 바인딩, 생성자 주입=테스트 주입점). 재시도는 연결·5xx·429만(타임아웃 제외).
- 테스트: MockWebServer 픽스처로 성공·5xx·타임아웃·refusal 경로(Story 3.1).

## 3. 보육일지 content 스키마 (FR-2/FR-3 — api-spec §9 확정)

**AI 원문 출력 스키마**(일지 분석 패스: 일지 서술 + 메모 영역분류를 1회 호출로):
```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["summary", "areas", "memoClassifications"],
  "properties": {
    "summary": { "type": "string" },
    "areas": {
      "type": "object", "additionalProperties": false,
      "required": ["PHYSICAL_HEALTH","COMMUNICATION","SOCIAL","ART","NATURE"],
      "properties": {
        "PHYSICAL_HEALTH": {"type":"string"}, "COMMUNICATION": {"type":"string"},
        "SOCIAL": {"type":"string"}, "ART": {"type":"string"}, "NATURE": {"type":"string"}
      }
    },
    "memoClassifications": {
      "type": "array",
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["index","area"],
        "properties": {
          "index": {"type":"integer"},
          "area": {"type":"string","enum":["PHYSICAL_HEALTH","COMMUNICATION","SOCIAL","ART","NATURE"]}
        }
      }
    }
  }
}
```
- 입력 메모는 user 프롬프트에 `[1] …`, `[2] …` 정수 인덱스로 제시. AI는 각 인덱스의 영역을 `memoClassifications`로 반환(FR-2). 서버가 index→memo.id 매핑 후 `memo.curriculum_area` 갱신.
- **DB 저장형(daily_journal.content)** = api-spec [11] 응답 형태로 평탄화:
  ```json
  { "summary":"...", "PHYSICAL_HEALTH":"...", "COMMUNICATION":"...", "SOCIAL":"...", "ART":"...", "NATURE":"..." }
  ```
  즉 `areas`를 펼쳐 저장(파서가 변환). `memoClassifications`는 저장 안 하고 메모 영역 갱신에만 사용.

**AnalysisResult(파싱 타입, ai/dto):**
```java
public record JournalAnalysisResult(String summary, Map<CurriculumArea,String> areas,
                                    List<MemoArea> memoClassifications) {
    public record MemoArea(int index, CurriculumArea area) {}
}
```

**OutputValidator(일지) 규칙** — 실패 시 1회 재요청, 그래도 실패면 `AI_OUTPUT_INVALID`:
- `summary` non-blank
- `areas` 5개 키 모두 존재 & non-blank
- `memoClassifications`가 입력 메모 인덱스를 **빠짐없이** 커버, area enum 유효

## 4. 개인평가 content 스키마 (FR-8/FR-9)

**AI 원문 출력 스키마:**
```json
{
  "type":"object","additionalProperties":false,
  "required":["summary","areas"],
  "properties":{
    "summary":{"type":"string"},
    "areas":{
      "type":"array",
      "items":{"type":"object","additionalProperties":false,
        "required":["area","text"],
        "properties":{
          "area":{"type":"string","enum":["PHYSICAL_HEALTH","COMMUNICATION","SOCIAL","ART","NATURE"]},
          "text":{"type":"string"}
        }}
    }
  }
}
```
- 디자인 `childAnalysis`(summary + 관찰된 영역만)와 정합 — 5영역 전부 아닌 **관찰된 영역 subset** 허용.
- **DB 저장형(child_report.content)** = 위 JSON 그대로 직렬화 저장.
- **OutputValidator(평가)**: `summary` non-blank + `areas` ≥1 + 각 area enum 유효 + text non-blank. 실패 시 1회 재요청.

## 5. 비식별화 프로토콜 (NFR-1, ai/deid)

- **분석 경로에서만**. 메모 저장 경로는 미개입.
- `Deidentifier`: 분석 대상 아동 실명 → 요청 스코프 센티넬 `[[CHILD_n]]` 치환. `RestorationContext`가 n↔실명 매핑 보유(요청 스코프).
- **순서**: 메모 비식별화 → 프롬프트 구성 → AiClient → **원문 텍스트에서 [[CHILD_n]]→실명 복원** → JSON 파싱 → OutputValidator.
  - 복원을 파싱 전에 문자열 치환으로 수행(센티넬은 JSON 구문과 충돌 안 함).
  - 복원 후에도 `[[CHILD_` 잔존 시 `DEIDENTIFICATION_RESTORE_FAILED`.
- **로그 금지**: 프롬프트/응답/에러 로그에 실명·메모 원문 금지. 토큰화된 형태만.
- 한계(MVP): 메모 본문 내 *타 아동명*은 토큰화 못 함 → 반 명단 치환으로 보강(아키텍처 명시).

## 6. 프롬프트 템플릿 (resources/prompts/)

`PromptTemplateLoader`가 치환. 파일: `journal.txt`, `child-report.txt`(영역분류는 일지에 통합되어 `area-classify.txt`는 보류/선택).

**강제 요구(addendum §C — 범용 챗봇 대비 차별점):**
- 누리과정 5영역 기반 서술·분류
- 같은 주제라도 **다양한 놀이 제안**(신체·언어·미술·역할 등)
- **또래 상호작용** 과정을 서술에 포함
- **교사의 지원/발문**을 함께 작성
- 메모 3항목(놀이/의사소통·상호작용/수업태도)을 입력 신호로 활용
- 출력은 **지정 JSON 스키마만**(structured outputs가 강제, 프롬프트로도 재확인)

## 7. 오케스트레이션 & 동시성

- `AnalysisGuard`(단일 인스턴스 in-memory): 오케스트레이션 진입점(JournalService/ReportService 맨 앞)에서 사용자 키로 잡고 finally에서 해제. 진행 중 재요청 → `ANALYSIS_IN_PROGRESS`(409).
- **TX 경계**: `JournalService.analyze()` no-TX → 메모 조회(readOnly 짧은 TX) → 비식별화 → AiClient(TX 밖) → 복원·검증 → `JournalPersistService.save()`(REQUIRES_NEW: daily_journal upsert + journal_memo_link 재구성 + 메모 영역 갱신). `ReportService`도 동형.
- 월말(FR-9): `MonthlyReportScheduler` 반 전체 순회, 아이 1명 단위 TX·try-catch 격리, 메모 없는 아이 skip, UNIQUE(child_id,report_month) 멱등 skip, 순차 호출.

## 8. 미해결/후속

- 벤더 교체(Claude 등) 시 `AiClient` 두 번째 구현만 추가. structured output 표현 차이는 각 구현이 흡수(불가 시 프롬프트 JSON 강제 + 파싱으로 폴백).
- `AI_MODEL` 기본값은 구현 스토리(3.1) 시점의 최신 OpenAI 모델 ID로 확정(본 문서는 env 주입만 규정).
- 일지 재분석(FR-6) 시 memoClassifications 재적용 정책: 기존 수동 수정(Story 2.3) 영역을 덮어쓸지 — 구현 스토리(3.7)에서 확정(기본: 자동분류는 미분류·자동분류분만 갱신, 수동수정 보존 권장).
- 토큰/비용 모니터링(usage 로깅)·레이트리밋은 post-MVP.

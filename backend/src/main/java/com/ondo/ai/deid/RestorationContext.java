package com.ondo.ai.deid;

import com.ondo.common.exception.DeidentificationException;

import java.util.Map;

/**
 * 한 번의 분석 요청에만 유효한 가명↔실명 매핑(요청 스코프). {@link Deidentifier}가 반 명단으로 생성한다.
 * 실명은 외부 AI로 나가지 않고, 이 객체의 어떤 동작도 실명을 로깅하지 않는다(NFR-1).
 * 정본: docs/specs/ai-integration-spec.md §5.
 */
public final class RestorationContext {

    /** 센티넬 토큰 형식: [[CHILD_n]]. 복원 후 TOKEN_PREFIX 가 남아 있으면 모델이 토큰을 변형/환각한 것. */
    static final String TOKEN_PREFIX = "[[CHILD_";
    static final String TOKEN_SUFFIX = "]]";

    /** 매핑에 없는 잘 형성된 환각 센티넬([[CHILD_<digits>]]) — 실명이 아니므로 중립어로 치환한다. */
    private static final java.util.regex.Pattern UNMAPPED_SENTINEL =
            java.util.regex.Pattern.compile("\\[\\[CHILD_\\d+\\]\\]");
    private static final String NEUTRAL_NAME = "아이";

    /** 센티넬 토큰 조립의 단일 출처. Deidentifier 가 사용한다. */
    static String token(int n) {
        return TOKEN_PREFIX + n + TOKEN_SUFFIX;
    }

    /** 실명 → 토큰. 치환 시 부분문자열 오치환을 막기 위해 '긴 이름부터' 순서를 보존한다(LinkedHashMap). */
    private final Map<String, String> nameToToken;
    /** 토큰 → 실명. 복원용. */
    private final Map<String, String> tokenToName;

    RestorationContext(Map<String, String> nameToToken, Map<String, String> tokenToName) {
        this.nameToToken = nameToToken;
        this.tokenToName = tokenToName;
    }

    /** 치환용 매핑(긴 이름 우선). {@link Deidentifier#deidentify}만 사용한다. */
    Map<String, String> nameToToken() {
        return nameToToken;
    }

    /**
     * AI 원문(structured면 JSON 문자열)에서 [[CHILD_n]] → 실명 복원. JSON 파싱 '이전'에 문자열 치환으로 호출한다
     * (센티넬은 JSON 구문과 충돌하지 않음). 복원 후에도 [[CHILD_ 가 남으면 {@link DeidentificationException}.
     */
    public String restore(String text) {
        if (text == null) {
            return null;
        }
        String restored = text;
        for (Map.Entry<String, String> entry : tokenToName.entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        // 모델이 매핑에 없는 인덱스를 환각한 잘 형성된 센티넬([[CHILD_<digits>]])은 실명이 아니라 모델 artifact다.
        // 실명 누수가 아니므로 중립어로 치환해 흘려보낸다(가용성↑). 정본: deferred-work 하드닝 항목.
        restored = UNMAPPED_SENTINEL.matcher(restored).replaceAll(NEUTRAL_NAME);
        // 그래도 센티넬 접두가 남으면(형식 깨짐 등 진짜 이상) 방어적으로 차단 유지(NFR-1).
        if (restored.contains(TOKEN_PREFIX)) {
            // 메시지에 원문/실명을 넣지 않는다(로그 금지 정책 일관).
            throw new DeidentificationException("복원 후 미해결 센티넬이 남아 있습니다.");
        }
        return restored;
    }
}

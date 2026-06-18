package com.jaram.ai;

import com.jaram.ai.dto.AiRequest;

/**
 * 외부 LLM 추상화(벤더 중립). 구현(OpenAiClient)은 인프라 — 정책 컴포넌트(비식별화·검증)는 이 인터페이스만 안다.
 */
public interface AiClient {

    /**
     * 프롬프트를 보내고 모델 원문 텍스트(structured면 JSON 문자열)를 반환.
     * 타임아웃/재시도/HTTP 매핑은 구현 책임. 실패 시 {@link com.jaram.common.exception.AiAnalysisException}.
     */
    String complete(AiRequest request);
}

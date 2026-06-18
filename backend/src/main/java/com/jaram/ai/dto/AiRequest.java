package com.jaram.ai.dto;

import java.util.Map;

/**
 * AiClient 호출 입력(벤더 중립). responseSchema 가 있으면 structured outputs(JSON 스키마)로 형태를 강제한다.
 *
 * @param systemPrompt   system 메시지(프롬프트 템플릿)
 * @param userPrompt     user 메시지(비식별화된 분석 입력)
 * @param responseSchema JSON 스키마(object). null 이면 일반 텍스트 응답
 * @param maxTokens      출력 토큰 상한
 */
public record AiRequest(
        String systemPrompt,
        String userPrompt,
        Map<String, Object> responseSchema,
        int maxTokens
) {
}

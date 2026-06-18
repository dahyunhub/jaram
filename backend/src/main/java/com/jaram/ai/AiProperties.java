package com.jaram.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 LLM 설정. application.yml 의 jaram.ai.* 바인딩.
 *
 * @param baseUrl        API 호스트(경로 제외, 예: https://api.openai.com)
 * @param apiKey         API 키(운영은 env 주입)
 * @param model          모델 ID
 * @param timeoutSeconds 서버 하드 타임아웃(읽기) — 프록시보다 짧게
 * @param maxRetries     재시도 횟수(연결 실패·5xx·429에만; 타임아웃 제외)
 */
@ConfigurationProperties(prefix = "jaram.ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds,
        int maxRetries
) {
}

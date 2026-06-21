package com.ondo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ondo.ai.dto.AiRequest;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 기반 AiClient(인프라). RestClient raw HTTP.
 * - structured outputs: response_format json_schema(strict)
 * - 하드 타임아웃(읽기) 적용, 재시도는 연결 실패·5xx·429에만(타임아웃 제외)
 * - 생성자 주입(AiProperties)이 테스트 주입점 — MockWebServer URL 로 교체 가능
 */
@Component
public class OpenAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final long BACKOFF_BASE_MS = 200L;
    private static final long CONNECT_TIMEOUT_SECONDS = 3L;

    private final RestClient rest;
    private final String url;
    private final String model;
    private final int maxRetries;

    public OpenAiClient(AiProperties props) {
        // 응답 대기(read)는 설정값, 연결(connect)은 짧게 분리 — 연결 단계는 빠르게 끊고 응답만 길게 기다린다.
        long readTimeout = Math.max(1, props.timeoutSeconds());
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(CONNECT_TIMEOUT_SECONDS, readTimeout)));
        factory.setReadTimeout(Duration.ofSeconds(readTimeout));
        this.rest = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + (props.apiKey() == null ? "" : props.apiKey()))
                .build();
        this.url = trimSlash(props.baseUrl()) + "/v1/chat/completions";
        this.model = props.model();
        this.maxRetries = Math.max(0, props.maxRetries());

        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("AI api-key 미설정(ondo.ai.api-key / AI_API_KEY) — 실제 호출 시 401 로 실패한다");
        }
        if (this.model == null || this.model.isBlank()) {
            log.warn("AI model 미설정(ondo.ai.model / AI_MODEL) — 실제 호출 시 4xx 로 실패한다");
        }
    }

    @Override
    public String complete(AiRequest request) {
        Map<String, Object> body = buildBody(request);
        AiAnalysisException lastFailure = null; // 재시도 소진 시 마지막 원인을 보존(진단용)
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return extractContent(post(body));
            } catch (AiAnalysisException e) {
                throw e; // refusal·빈 응답·타임아웃은 종결(재시도 안 함)
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status != 429 && status < 500) {
                    log.warn("AI 호출 4xx({}) — 재시도 안 함", status);
                    throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED, e);
                }
                log.warn("AI 호출 {}xx({}) — 재시도 {}/{}", status / 100, status, attempt + 1, maxRetries + 1);
                lastFailure = new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED, e);
            } catch (ResourceAccessException e) {
                if (isTimeout(e)) {
                    log.warn("AI 호출 타임아웃 — 재시도 안 함");
                    throw new AiAnalysisException(ErrorCode.AI_TIMEOUT, e);
                }
                log.warn("AI 호출 연결 실패 — 재시도 {}/{}", attempt + 1, maxRetries + 1);
                lastFailure = new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED, e);
            } catch (RestClientException e) {
                // 본문 추출 중 읽기 타임아웃 등은 RestClientException 으로 래핑됨
                if (isTimeout(e)) {
                    log.warn("AI 호출 타임아웃(본문) — 재시도 안 함");
                    throw new AiAnalysisException(ErrorCode.AI_TIMEOUT, e);
                }
                log.warn("AI 응답 처리 실패 — 재시도 안 함");
                throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED, e);
            }
            if (attempt < maxRetries) backoff(attempt);
        }
        throw lastFailure != null ? lastFailure : new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED);
    }

    private OpenAiChatResponse post(Map<String, Object> body) {
        return rest.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiChatResponse.class);
    }

    private Map<String, Object> buildBody(AiRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", req.maxTokens());
        body.put("messages", List.of(
                Map.of("role", "system", "content", req.systemPrompt()),
                Map.of("role", "user", "content", req.userPrompt())));
        if (req.responseSchema() != null) {
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of("name", "ondo_result", "strict", true, "schema", req.responseSchema())));
        }
        return body;
    }

    private String extractContent(OpenAiChatResponse resp) {
        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED);
        }
        OpenAiChatResponse.Choice choice = resp.choices().get(0);
        OpenAiChatResponse.Message msg = choice.message();
        if (msg == null) {
            throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED);
        }
        if (msg.refusal() != null && !msg.refusal().isBlank()) {
            log.warn("AI 거절(refusal) 응답");
            throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED);
        }
        if (msg.content() == null || msg.content().isBlank()) {
            throw new AiAnalysisException(ErrorCode.AI_ANALYSIS_FAILED);
        }
        // 정상처럼 보이는 content 라도 비정상 종료(finish_reason != stop)면 신뢰 불가.
        // length 절단은 깨진 JSON 으로 파서가 잡기도 하지만, 여기서 명시적으로 차단한다.
        // (finish_reason 미제공 응답은 허용 — 하위 호환)
        String finish = choice.finishReason();
        if (finish != null && !finish.isBlank() && !"stop".equals(finish)) {
            log.warn("AI 비정상 종료(finish_reason={}) — content 신뢰 불가", finish);
            // length(절단)은 불완전 출력 → 1회 재요청 대상(AI_OUTPUT_INVALID), 그 외는 재요청 무의미
            throw new AiAnalysisException(
                    "length".equals(finish) ? ErrorCode.AI_OUTPUT_INVALID : ErrorCode.AI_ANALYSIS_FAILED);
        }
        return msg.content();
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) return true;
        }
        return false;
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(BACKOFF_BASE_MS * (1L << attempt), 2000L));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** OpenAI Chat Completions 응답(필요 필드만). 미사용 필드는 무시. */
    record OpenAiChatResponse(List<Choice> choices) {
        record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {}
        record Message(String content, String refusal) {}
    }
}

package com.ondo.ai;

import com.ondo.ai.dto.AiRequest;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * OpenAiClient 단위 테스트 — MockWebServer 로 OpenAI HTTP 경로(성공·거절·5xx·타임아웃) 검증. 스프링/DB 불필요.
 */
class OpenAiClientTest {

    private MockWebServer server;
    private OpenAiClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        // timeoutSeconds=1, maxRetries=1 → 5xx 시 2회 시도
        AiProperties props = new AiProperties(server.url("/").toString(), "test-key", "gpt-test", 1, 1);
        client = new OpenAiClient(props);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private AiRequest req() {
        return new AiRequest("system 프롬프트", "user 메모 묶음", null, 256);
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    void 성공하면_message_content_를_반환한다() {
        server.enqueue(json(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"summary\\\":\\\"ok\\\"}\"}}]}"));

        String out = client.complete(req());

        assertThat(out).isEqualTo("{\"summary\":\"ok\"}");
    }

    @Test
    void 거절_refusal_응답은_AI_ANALYSIS_FAILED() {
        server.enqueue(json(200, "{\"choices\":[{\"message\":{\"refusal\":\"거절\",\"content\":null}}]}"));

        AiAnalysisException ex = catchThrowableOfType(() -> client.complete(req()), AiAnalysisException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
    }

    @Test
    void 서버_5xx_가_재시도_소진되면_AI_ANALYSIS_FAILED() {
        server.enqueue(json(500, "{}"));
        server.enqueue(json(500, "{}")); // maxRetries=1 → 2회 모두 실패

        AiAnalysisException ex = catchThrowableOfType(() -> client.complete(req()), AiAnalysisException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void 서버_5xx_후_재시도_성공() {
        server.enqueue(json(500, "{}"));
        server.enqueue(json(200, "{\"choices\":[{\"message\":{\"content\":\"recovered\"}}]}"));

        String out = client.complete(req());

        assertThat(out).isEqualTo("recovered");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void 일반_4xx_는_재시도없이_AI_ANALYSIS_FAILED() {
        server.enqueue(json(400, "{\"error\":\"bad request\"}"));

        AiAnalysisException ex = catchThrowableOfType(() -> client.complete(req()), AiAnalysisException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
        assertThat(server.getRequestCount()).isEqualTo(1); // 4xx(429 제외)는 재시도 안 함
    }

    @Test
    void 서버_429_후_재시도_성공() {
        server.enqueue(json(429, "{\"error\":\"rate limited\"}"));
        server.enqueue(json(200, "{\"choices\":[{\"message\":{\"content\":\"recovered\"}}]}"));

        String out = client.complete(req());

        assertThat(out).isEqualTo("recovered"); // 429 는 재시도 대상
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void 연결_실패_타임아웃아님_는_재시도후_AI_ANALYSIS_FAILED() throws Exception {
        // 아무도 listen 하지 않는 포트 → connection refused(ConnectException, SocketTimeoutException 아님) → 재시도 대상
        int deadPort;
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            deadPort = s.getLocalPort();
        }
        AiProperties props = new AiProperties("http://localhost:" + deadPort, "test-key", "gpt-test", 1, 1);
        OpenAiClient deadClient = new OpenAiClient(props);

        AiAnalysisException ex = catchThrowableOfType(() -> deadClient.complete(req()), AiAnalysisException.class);

        // 연결 실패는 타임아웃(AI_TIMEOUT)이 아니라 재시도 소진 후 AI_ANALYSIS_FAILED 로 종결
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_ANALYSIS_FAILED);
    }

    @Test
    void 타임아웃은_재시도없이_AI_TIMEOUT() {
        // read timeout(1s)보다 긴 지연 → SocketTimeoutException → AI_TIMEOUT, 재시도 안 함
        server.enqueue(json(200, "{\"choices\":[{\"message\":{\"content\":\"늦음\"}}]}")
                .setBodyDelay(3, TimeUnit.SECONDS));

        AiAnalysisException ex = catchThrowableOfType(() -> client.complete(req()), AiAnalysisException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_TIMEOUT);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}

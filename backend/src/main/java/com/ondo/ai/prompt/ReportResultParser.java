package com.ondo.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.ai.dto.ReportAnalysisResult;
import com.ondo.common.exception.AiAnalysisException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 복원이 끝난 AI 원문(JSON) → {@link ReportAnalysisResult}. 정본: ai-integration-spec §4.
 * 파싱 실패 시 AI_OUTPUT_INVALID. 인프라(OpenAiClient)를 모른다. {@code JournalResultParser} 미러.
 */
@Component
public class ReportResultParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportAnalysisResult parse(String restoredJson) {
        try {
            return objectMapper.readValue(restoredJson, ReportAnalysisResult.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new AiAnalysisException(ErrorCode.AI_OUTPUT_INVALID);
        }
    }
}

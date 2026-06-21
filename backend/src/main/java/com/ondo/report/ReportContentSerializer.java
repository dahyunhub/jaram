package com.ondo.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.ai.dto.ReportAnalysisResult;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 결과({summary, areas:[{area,text}]}) → 저장 JSON / 저장 JSON → 응답 content 객체.
 * CurriculumArea enum 은 Jackson 이 name 문자열로 직렬화 → 저장 스키마(ai-integration-spec §4)와 일치.
 * ObjectMapper 는 자체 인스턴스(주입 가능 빈 없음 — Story 3.3 교훈). {@code JournalContentSerializer} 미러.
 */
@Component
public class ReportContentSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** child_report.content 저장용 JSON 직렬화. */
    public String toJson(ReportAnalysisResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /** 저장 JSON → 응답용 구조화 content 객체({summary, areas:[{area,text}]}). */
    public Object toContent(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}

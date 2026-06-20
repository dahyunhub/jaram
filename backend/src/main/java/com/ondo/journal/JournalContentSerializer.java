package com.ondo.journal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.ai.dto.JournalAnalysisResult;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.memo.domain.CurriculumArea;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 결과(summary + areas{5}) → 저장·응답용 평탄화 content({summary, PHYSICAL_HEALTH, …, NATURE}).
 * 정본: ai-integration-spec §3(저장형 평탄화). ObjectMapper 는 자체 인스턴스(주입 가능 빈 없음 — Story 3.3 교훈).
 */
@Component
public class JournalContentSerializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 평탄화 content 맵(순서 고정: summary → 5영역). */
    public Map<String, String> toFlatContent(JournalAnalysisResult result) {
        Map<String, String> flat = new LinkedHashMap<>();
        flat.put("summary", result.summary());
        for (CurriculumArea area : CurriculumArea.values()) {
            flat.put(area.name(), result.areas().get(area));
        }
        return flat;
    }

    /** 평탄화 content 를 daily_journal.content 저장용 JSON 문자열로 직렬화. */
    public String toJson(Map<String, String> flatContent) {
        try {
            return objectMapper.writeValueAsString(flatContent);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}

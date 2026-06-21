package com.ondo.ai.prompt;

import com.ondo.memo.domain.CurriculumArea;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 개인평가 응답의 JSON 스키마(ai-integration-spec §4). OpenAI structured outputs(strict)용.
 * strict 요건: 모든 property required + additionalProperties:false. areas 는 {area(enum), text} 객체의 배열.
 */
public final class ReportAnalysisSchema {

    private ReportAnalysisSchema() {
    }

    private static final List<String> AREA_NAMES =
            Arrays.stream(CurriculumArea.values()).map(Enum::name).toList();

    /** {summary, areas:[{area, text}]} 스키마. */
    public static Map<String, Object> schema() {
        Map<String, Object> areaItem = object(
                List.of("area", "text"),
                Map.of(
                        "area", Map.of("type", "string", "enum", AREA_NAMES),
                        "text", Map.of("type", "string")));
        Map<String, Object> areas = Map.of("type", "array", "items", areaItem);
        return object(
                List.of("summary", "areas"),
                Map.of("summary", Map.of("type", "string"), "areas", areas));
    }

    private static Map<String, Object> object(List<String> required, Map<String, Object> properties) {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("type", "object");
        obj.put("additionalProperties", false);
        obj.put("required", required);
        obj.put("properties", properties);
        return obj;
    }
}

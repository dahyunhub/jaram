package com.ondo.ai.dto;

import com.ondo.memo.domain.CurriculumArea;

import java.util.List;

/**
 * 개인평가 분석 AI 응답의 파싱 타입(ai-integration-spec §4). 복원된 JSON 문자열에서 매핑된다.
 * 일지와 달리 areas 는 '관찰된 영역 subset' 배열({area, text})이고 5영역 강제가 아니다.
 *
 * @param summary 개인 관찰 평가 요약 서술
 * @param areas   관찰된 누리 영역별 서술(subset)
 */
public record ReportAnalysisResult(
        String summary,
        List<AreaText> areas
) {
    /**
     * @param area 누리 영역
     * @param text 해당 영역 관찰 서술
     */
    public record AreaText(CurriculumArea area, String text) {
    }
}

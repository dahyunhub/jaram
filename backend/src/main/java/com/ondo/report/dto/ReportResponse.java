package com.ondo.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 개인평가 단건 응답(api-spec [16]/[18]). content 는 구조화 객체({summary, areas:[{area,text}]}).
 */
public record ReportResponse(
        Long id,
        Long childId,
        String reportType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String reportMonth,
        Object content,
        LocalDateTime createdAt
) {
}

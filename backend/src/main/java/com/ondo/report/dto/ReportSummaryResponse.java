package com.ondo.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 개인평가 목록 응답(api-spec [17]). content 는 생략(요약 리스트).
 */
public record ReportSummaryResponse(
        Long id,
        String reportType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String reportMonth,
        LocalDateTime createdAt
) {
}

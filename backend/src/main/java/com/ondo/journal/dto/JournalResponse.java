package com.ondo.journal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 일지 응답(api-spec [11]). content 는 평탄화 5영역 객체({summary, PHYSICAL_HEALTH, …, NATURE}).
 */
public record JournalResponse(
        Long id,
        Long classroomId,
        LocalDate journalDate,
        String status,
        Map<String, String> content,
        LocalDateTime analyzedAt,
        List<Long> linkedMemoIds
) {
}

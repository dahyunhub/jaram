package com.ondo.journal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 하루치 일지 초안 생성 요청(api-spec [11]). 부분 선택 없음 — 반 전체·해당 날짜 메모를 묶는다.
 */
public record JournalAnalyzeRequest(
        @NotNull Long classroomId,
        @NotNull LocalDate date
) {
}

package com.ondo.classroom.dto;

import java.time.LocalDate;

/**
 * 담당 반 목록 응답(API [2]). childCount = soft delete 안 된 아이 수.
 */
public record ClassroomResponse(
        Long id,
        String name,
        Integer year,
        LocalDate startDate,
        long childCount
) {
}

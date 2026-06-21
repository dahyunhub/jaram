package com.ondo.classroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 반 생성 요청(반 추가하기). start_date 는 받지 않고 학년도 3월 2일로 자동 설정(한국 학년도 시작).
 */
public record ClassroomCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(2000) @Max(2100) Integer year
) {
}

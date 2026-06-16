package com.jaram.child.dto;

import com.jaram.child.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 아이 등록·수정 요청(API [4]/[5]).
 */
public record ChildRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Past @BirthDateRange LocalDate birthDate,
        @NotNull Gender gender
) {
}

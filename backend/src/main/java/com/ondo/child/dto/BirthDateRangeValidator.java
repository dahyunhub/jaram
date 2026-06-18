package com.ondo.child.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class BirthDateRangeValidator implements ConstraintValidator<BirthDateRange, LocalDate> {

    private int maxAgeYears;

    @Override
    public void initialize(BirthDateRange constraint) {
        this.maxAgeYears = constraint.maxAgeYears();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 은 @NotNull 이 처리
        }
        // 미래/오늘은 @Past 가 처리하므로 하한만 검증
        return !value.isBefore(LocalDate.now().minusYears(maxAgeYears));
    }
}

package com.jaram.child.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 생년월일 하한 검증. 미래/오늘은 {@code @Past} 가 처리하고, 여기서는 비현실적으로 과거인 값을 거른다.
 * 보육 도메인상 유치원 연령(기본 12년) 범위를 벗어나면 검증 실패.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BirthDateRangeValidator.class)
public @interface BirthDateRange {

    String message() default "생년월일이 유효한 범위를 벗어났어요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int maxAgeYears() default 12;
}

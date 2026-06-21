package com.ondo.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 문자열의 인코딩 바이트 길이 상한 제약. {@code @Size}(문자 수)와 달리 멀티바이트(한글 등)에서도
 * 실제 바이트 수를 보장한다. 예: BCrypt 72바이트 한계 보호(password).
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface MaxBytes {

    int value();

    String charset() default "UTF-8";

    String message() default "허용 길이를 초과했어요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

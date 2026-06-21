package com.ondo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.Charset;

/**
 * {@link MaxBytes} 검증기. null 은 통과(널 여부는 @NotBlank/@NotNull 책임).
 */
public class MaxBytesValidator implements ConstraintValidator<MaxBytes, String> {

    private int max;
    private Charset charset;

    @Override
    public void initialize(MaxBytes annotation) {
        this.max = annotation.value();
        this.charset = Charset.forName(annotation.charset());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.getBytes(charset).length <= max;
    }
}

package com.ondo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * 표준 에러 응답 포맷. 정본: docs/specs/error-code-catalog.md §1.
 * { timestamp, status, code, message, path } + 검증 실패 시 errors[].
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> errors
) {

    public record FieldErrorDetail(String field, String reason) {
    }

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now().toString(), status, code, message, path, null);
    }

    public static ApiError of(int status, String code, String message, String path, List<FieldErrorDetail> errors) {
        return new ApiError(Instant.now().toString(), status, code, message, path,
                (errors == null || errors.isEmpty()) ? null : errors);
    }
}

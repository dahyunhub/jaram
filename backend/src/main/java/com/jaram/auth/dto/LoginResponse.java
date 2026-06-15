package com.jaram.auth.dto;

/**
 * 로그인 성공 응답. 정본: docs/specs/api-spec.md [1].
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        TeacherSummary teacher
) {

    public record TeacherSummary(Long id, String email, String name) {
    }
}

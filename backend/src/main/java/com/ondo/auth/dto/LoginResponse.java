package com.ondo.auth.dto;

import java.time.LocalDateTime;

/**
 * 로그인 성공 응답. 정본: docs/specs/api-spec.md [1].
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        TeacherSummary teacher
) {

    /** photoUpdatedAt: 교사 프로필 사진 갱신시각(없으면 null) — 프론트 아바타 표시·캐시 키. */
    public record TeacherSummary(Long id, String email, String name, LocalDateTime photoUpdatedAt) {
    }
}

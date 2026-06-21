package com.ondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청(FR-12, NFR-6). 비밀번호는 평문으로 받아 서버에서 BCrypt 해시한다.
 * 응답은 로그인과 동일한 {@link LoginResponse}(가입 즉시 자동 로그인).
 */
public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {
}

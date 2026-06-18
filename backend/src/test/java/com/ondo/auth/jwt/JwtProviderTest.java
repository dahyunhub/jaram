package com.ondo.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtProvider 순수 단위 테스트(스프링 컨텍스트·DB 불필요).
 */
class JwtProviderTest {

    private static final String SECRET = "unit-test-secret-0123456789abcdef0123456789abcdef";

    @Test
    void 토큰을_발급하고_teacherId_를_복원한다() {
        JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, 3600));

        String token = provider.createToken(42L, "teacher@ondo.dev");

        assertThat(provider.parseTeacherId(token)).isEqualTo(42L);
        assertThat(provider.getExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    void 만료된_토큰은_ExpiredJwtException_을_던진다() {
        JwtProvider provider = new JwtProvider(new JwtProperties(SECRET, -1));

        String expired = provider.createToken(1L, "teacher@ondo.dev");

        assertThatThrownBy(() -> provider.parseTeacherId(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 잘못된_서명의_토큰은_거부된다() {
        JwtProvider issuer = new JwtProvider(new JwtProperties(SECRET, 3600));
        JwtProvider other = new JwtProvider(new JwtProperties("another-secret-0123456789abcdef0123456789abcdef", 3600));

        String token = issuer.createToken(1L, "teacher@ondo.dev");

        assertThatThrownBy(() -> other.parseTeacherId(token))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
}

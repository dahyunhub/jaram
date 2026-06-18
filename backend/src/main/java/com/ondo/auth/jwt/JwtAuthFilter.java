package com.ondo.auth.jwt;

import com.ondo.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bearer JWT 를 파싱해 SecurityContext 에 인증을 채운다. 무상태.
 * 토큰이 없거나 무효면 그대로 통과시키고, 보호 경로 접근 시 SecurityConfig 의 EntryPoint 가 401 로 응답한다.
 * 만료/무효는 요청 속성으로 구분해 EntryPoint 가 적절한 ErrorCode 를 고른다.
 *
 * 빈으로 등록하지 않고 SecurityConfig 에서 직접 생성한다(서블릿 체인 자동 등록 방지).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "ondo.authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Long teacherId = jwtProvider.parseTeacherId(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        teacherId, null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute(ATTR_AUTH_ERROR, ErrorCode.AUTH_TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ATTR_AUTH_ERROR, ErrorCode.AUTH_UNAUTHENTICATED);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

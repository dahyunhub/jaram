package com.jaram.auth.jwt;

import com.jaram.common.exception.ErrorCode;
import com.jaram.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 미인증 접근(401)을 표준 에러 응답으로 반환한다.
 * JwtAuthFilter 가 만료/무효를 구분해 둔 요청 속성이 있으면 그 ErrorCode 를, 없으면 AUTH_UNAUTHENTICATED 를 사용한다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object attr = request.getAttribute(JwtAuthFilter.ATTR_AUTH_ERROR);
        ErrorCode code = (attr instanceof ErrorCode ec) ? ec : ErrorCode.AUTH_UNAUTHENTICATED;

        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());

        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}

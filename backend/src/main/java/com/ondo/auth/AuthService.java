package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.LoginResponse;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest request) {
        Teacher teacher = teacherRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createToken(teacher.getId(), teacher.getEmail());
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtProvider.getExpirationSeconds(),
                new LoginResponse.TeacherSummary(teacher.getId(), teacher.getEmail(), teacher.getName()));
    }
}

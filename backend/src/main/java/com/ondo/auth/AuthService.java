package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.LoginResponse;
import com.ondo.auth.dto.RegisterRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.photo.ProfilePhotoService;
import com.ondo.photo.domain.OwnerKind;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ProfilePhotoService photoService;

    public AuthService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       ProfilePhotoService photoService) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.photoService = photoService;
    }

    public LoginResponse login(LoginRequest request) {
        Teacher teacher = teacherRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), teacher.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        return issueToken(teacher);
    }

    /**
     * 회원가입(FR-12). 이메일 중복은 EMAIL_ALREADY_EXISTS(409). 비밀번호는 BCrypt 해시로 저장하고,
     * 가입 성공 시 로그인과 동일한 토큰을 발급해 자동 로그인시킨다.
     * 클래스가 readOnly 라 쓰기 트랜잭션을 명시한다.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (teacherRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Teacher teacher = teacherRepository.save(
                Teacher.create(request.email(), passwordEncoder.encode(request.password()), request.name()));
        return issueToken(teacher);
    }

    /** 로그인·가입 공통 토큰 발급(자동 로그인 응답 조립). */
    private LoginResponse issueToken(Teacher teacher) {
        String accessToken = jwtProvider.createToken(teacher.getId(), teacher.getEmail());
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtProvider.getExpirationSeconds(),
                new LoginResponse.TeacherSummary(teacher.getId(), teacher.getEmail(), teacher.getName(),
                        photoService.updatedAtOrNull(OwnerKind.TEACHER, teacher.getId())));
    }
}

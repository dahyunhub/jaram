package com.jaram.auth;

import com.jaram.auth.domain.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * dev 프로파일 전용 시드 교사. 운영(prod)·테스트에는 실행되지 않는다(시크릿 노출 방지, data-model-spec §5).
 * 로그인 데모용: teacher@jaram.dev / password1234
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private static final String DEV_EMAIL = "teacher@jaram.dev";
    private static final String DEV_RAW_PASSWORD = "password1234";

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (teacherRepository.findByEmail(DEV_EMAIL).isPresent()) {
            return;
        }
        Teacher teacher = Teacher.create(DEV_EMAIL, passwordEncoder.encode(DEV_RAW_PASSWORD), "민지");
        teacherRepository.save(teacher);
        log.info("[dev] 시드 교사 생성: {} / {}", DEV_EMAIL, DEV_RAW_PASSWORD);
    }
}

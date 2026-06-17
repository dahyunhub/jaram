package com.jaram.auth;

import com.jaram.auth.domain.Teacher;
import com.jaram.child.ChildRepository;
import com.jaram.child.domain.Child;
import com.jaram.child.domain.Gender;
import com.jaram.classroom.ClassroomRepository;
import com.jaram.classroom.domain.Classroom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * dev 프로파일 전용 시드 데이터. 운영(prod)·테스트에는 실행되지 않는다(시크릿 노출 방지, data-model-spec §5).
 * 로그인 데모용 교사 + 반 + 아이 명단을 한 번 주입해 로그인→반 선택→아이 CRUD 흐름을 바로 시연한다.
 * 로그인: teacher@jaram.dev / password1234
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private static final String DEV_EMAIL = "teacher@jaram.dev";
    private static final String DEV_RAW_PASSWORD = "password1234";

    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final ChildRepository childRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(TeacherRepository teacherRepository,
                              ClassroomRepository classroomRepository,
                              ChildRepository childRepository,
                              PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.childRepository = childRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Teacher teacher = teacherRepository.findByEmail(DEV_EMAIL).orElse(null);
        if (teacher == null) {
            teacher = teacherRepository.save(
                    Teacher.create(DEV_EMAIL, passwordEncoder.encode(DEV_RAW_PASSWORD), "민지"));
            log.info("[dev] 시드 교사 생성: {} / {}", DEV_EMAIL, DEV_RAW_PASSWORD);
        }

        // 이미 반이 있으면 더 시드하지 않음(멱등)
        if (!classroomRepository.findClassroomSummaryRows(teacher.getId()).isEmpty()) {
            return;
        }

        Long classroomId = classroomRepository.save(
                Classroom.create(teacher.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();

        record Seed(String name, LocalDate birth, Gender gender) {}
        List<Seed> kids = List.of(
                new Seed("강하준", LocalDate.of(2020, 4, 12), Gender.MALE),
                new Seed("김민준", LocalDate.of(2020, 11, 3), Gender.MALE),
                new Seed("박서윤", LocalDate.of(2021, 2, 27), Gender.FEMALE),
                new Seed("이도윤", LocalDate.of(2021, 1, 8), Gender.MALE),
                new Seed("정시우", LocalDate.of(2020, 9, 30), Gender.FEMALE),
                new Seed("최아인", LocalDate.of(2020, 5, 21), Gender.FEMALE));

        char alias = 'A';
        for (Seed s : kids) {
            childRepository.save(Child.create(classroomId, s.name(), s.birth(), s.gender(), "아이" + alias));
            alias++;
        }
        log.info("[dev] 시드 반(햇살반) + 아이 {}명 생성", kids.size());
    }
}

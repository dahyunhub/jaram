package com.ondo.classroom;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.classroom.domain.Classroom;
import com.ondo.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ClassroomIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenA;

    @BeforeEach
    void setUp() {
        Teacher teacherA = teacherRepository.save(Teacher.create("a@ondo.dev", "hash", "교사A"));
        Teacher teacherB = teacherRepository.save(Teacher.create("b@ondo.dev", "hash", "교사B"));

        // 교사A: 동명 반 2개(학년도 구분)
        Classroom sunny2026 = classroomRepository.save(
                Classroom.create(teacherA.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2)));
        Classroom sunny2025 = classroomRepository.save(
                Classroom.create(teacherA.getId(), "햇살반", 2025, LocalDate.of(2025, 3, 2)));
        // 교사B: 다른 반(소유권 테스트용)
        classroomRepository.save(
                Classroom.create(teacherB.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2)));

        // 2026 반: 활성 2 + soft delete 1 → childCount 2
        insertChild(sunny2026.getId(), "아이1", "s26-1", false);
        insertChild(sunny2026.getId(), "아이2", "s26-2", false);
        insertChild(sunny2026.getId(), "아이3", "s26-3", true);
        // 2025 반: 활성 1 → childCount 1
        insertChild(sunny2025.getId(), "아이4", "s25-1", false);

        tokenA = jwtProvider.createToken(teacherA.getId(), teacherA.getEmail());
    }

    @Test
    void 본인_소유_반만_아이수와_함께_학년도_내림차순으로_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // year DESC → 2026 먼저
                .andExpect(jsonPath("$[0].name").value("햇살반"))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].startDate").value("2026-03-02"))
                .andExpect(jsonPath("$[0].childCount").value(2))
                .andExpect(jsonPath("$[1].year").value(2025))
                .andExpect(jsonPath("$[1].childCount").value(1));
    }

    @Test
    void 새_반을_생성하면_201과_반정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"별빛반\",\"year\":2026}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("별빛반"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.startDate").value("2026-03-02")) // 학년도 3월 2일 자동
                .andExpect(jsonPath("$.childCount").value(0));

        // 목록에도 추가됨(교사A: 기존 2 + 1 = 3)
        mockMvc.perform(get("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void 같은_학년도_같은_이름_반은_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"햇살반\",\"year\":2026}")) // 이미 존재
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 토큰_없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/api/v1/classrooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHENTICATED"));
    }

    private void insertChild(Long classroomId, String name, String alias, boolean deleted) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp deletedAt = deleted ? now : null;
        jdbcTemplate.update("""
                        INSERT INTO child (classroom_id, name, birth_date, gender, token_alias, deleted_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                classroomId, name, Date.valueOf(LocalDate.of(2021, 4, 10)), "MALE", alias, deletedAt, now, now);
    }
}

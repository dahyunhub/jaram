package com.jaram.child;

import com.jaram.auth.TeacherRepository;
import com.jaram.auth.domain.Teacher;
import com.jaram.auth.jwt.JwtProvider;
import com.jaram.child.domain.Child;
import com.jaram.child.domain.Gender;
import com.jaram.child.dto.ChildRequest;
import com.jaram.classroom.ClassroomRepository;
import com.jaram.classroom.domain.Classroom;
import com.jaram.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ChildIntegrationTest extends IntegrationTestSupport {

    private static final LocalDate BIRTH = LocalDate.of(2021, 4, 10);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private ClassroomRepository classroomRepository;
    @Autowired
    private ChildRepository childRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private ObjectMapper objectMapper;
    @PersistenceContext
    private EntityManager em;

    private String tokenA;
    private Long classroomAId;
    private Long classroomBId;
    private Long childBId;

    @BeforeEach
    void setUp() {
        Teacher teacherA = teacherRepository.save(Teacher.create("a@jaram.dev", "hash", "교사A"));
        Teacher teacherB = teacherRepository.save(Teacher.create("b@jaram.dev", "hash", "교사B"));
        classroomAId = classroomRepository.save(
                Classroom.create(teacherA.getId(), "햇살반", 2026, LocalDate.of(2026, 3, 2))).getId();
        classroomBId = classroomRepository.save(
                Classroom.create(teacherB.getId(), "달님반", 2026, LocalDate.of(2026, 3, 2))).getId();
        childBId = childRepository.save(Child.create(classroomBId, "남의아이", BIRTH, Gender.MALE, "아이A")).getId();
        tokenA = jwtProvider.createToken(teacherA.getId(), teacherA.getEmail());
    }

    private String body(String name, LocalDate birthDate) {
        return objectMapper.writeValueAsString(new ChildRequest(name, birthDate, Gender.MALE));
    }

    @Test
    void 아이를_등록하면_201과_token_alias가_부여된다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("김민준", BIRTH)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("김민준"))
                .andExpect(jsonPath("$.birthDate").value("2021-04-10"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.tokenAlias").value("아이A"));
    }

    @Test
    void 성별이_응답에_그대로_반영된다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChildRequest("박서윤", BIRTH, Gender.FEMALE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }

    @Test
    void 성별이_없으면_400_VALIDATION_FAILED() throws Exception {
        String noGender = "{\"name\":\"김민준\",\"birthDate\":\"2021-04-10\"}";
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(noGender))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 명단은_가나다순이고_삭제된_아이는_제외된다() throws Exception {
        register("박서연");   // 아이A
        register("김민준");   // 아이B

        mockMvc.perform(get("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("김민준"))
                .andExpect(jsonPath("$[1].name").value("박서연"));

        Long parkId = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomAId).stream()
                .filter(c -> c.getName().equals("박서연")).findFirst().orElseThrow().getId();
        mockMvc.perform(delete("/api/v1/children/{id}", parkId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("김민준"));
    }

    @Test
    void 삭제된_아이의_alias는_재사용되지_않는다() throws Exception {
        register("박서연");   // 아이A
        Long parkId = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomAId).getFirst().getId();
        mockMvc.perform(delete("/api/v1/children/{id}", parkId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // 삭제 아이도 alias 점유 → 다음은 아이B
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("이도윤", BIRTH)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenAlias").value("아이B"));
    }

    @Test
    void 아이_정보를_수정한다() throws Exception {
        register("김민준");
        Long id = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomAId).getFirst().getId();

        mockMvc.perform(put("/api/v1/children/{id}", id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("김민서", LocalDate.of(2020, 1, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김민서"))
                .andExpect(jsonPath("$.birthDate").value("2020-01-01"));
    }

    @Test
    void 이미_삭제된_아이를_다시_삭제하면_404() throws Exception {
        register("김민준");
        Long id = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomAId).getFirst().getId();
        mockMvc.perform(delete("/api/v1/children/{id}", id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());
        // 실제로는 요청마다 새 세션. 단일 트랜잭션 테스트에서 1차 캐시를 비워 같은 상황을 재현한다.
        em.flush();
        em.clear();
        mockMvc.perform(delete("/api/v1/children/{id}", id).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 타_교사_반에_등록하면_404_CLASSROOM_NOT_FOUND() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomBId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("김민준", BIRTH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLASSROOM_NOT_FOUND"));
    }

    @Test
    void 타_교사_아이를_수정하면_404_CHILD_NOT_FOUND() throws Exception {
        mockMvc.perform(put("/api/v1/children/{id}", childBId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("바꾼이름", BIRTH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHILD_NOT_FOUND"));
    }

    @Test
    void 이름이_비면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("  ", BIRTH)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 생년월일이_미래면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("김민준", LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 생년월일이_비현실적으로_과거면_400_VALIDATION_FAILED() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("김민준", LocalDate.now().minusYears(20))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void 이름_앞뒤_공백은_제거되어_저장된다() throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body("  김민준  ", BIRTH)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("김민준"));
    }

    private void register(String name) throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/{id}/children", classroomAId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body(name, BIRTH)))
                .andExpect(status().isCreated());
    }
}

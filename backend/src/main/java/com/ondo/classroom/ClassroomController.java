package com.ondo.classroom;

import com.ondo.classroom.dto.ClassroomCreateRequest;
import com.ondo.classroom.dto.ClassroomResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    /** 담당 반 목록(아이 수 포함). 인증된 교사 본인 소유만(API [2]). */
    @GetMapping
    public List<ClassroomResponse> getMyClassrooms(@AuthenticationPrincipal Long teacherId) {
        return classroomService.getMyClassrooms(teacherId);
    }

    /** 새 반 생성(반 추가하기) — 201. start_date 는 학년도 3월 2일 자동. */
    @PostMapping
    public ResponseEntity<ClassroomResponse> create(@AuthenticationPrincipal Long teacherId,
                                                    @Valid @RequestBody ClassroomCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classroomService.create(teacherId, request));
    }
}

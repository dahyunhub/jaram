package com.ondo.classroom;

import com.ondo.classroom.dto.ClassroomResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}

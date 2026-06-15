package com.jaram.child;

import com.jaram.child.dto.ChildRequest;
import com.jaram.child.dto.ChildResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 아이 등록·조회·수정·삭제(FR-11, API [3]~[6]). 소유권은 ChildService 에서 강제.
 */
@RestController
@RequestMapping("/api/v1")
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @GetMapping("/classrooms/{classroomId}/children")
    public List<ChildResponse> getChildren(@AuthenticationPrincipal Long teacherId,
                                           @PathVariable Long classroomId) {
        return childService.getChildren(teacherId, classroomId);
    }

    @PostMapping("/classrooms/{classroomId}/children")
    public ResponseEntity<ChildResponse> registerChild(@AuthenticationPrincipal Long teacherId,
                                                        @PathVariable Long classroomId,
                                                        @Valid @RequestBody ChildRequest request) {
        ChildResponse response = childService.registerChild(teacherId, classroomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/children/{childId}")
    public ChildResponse updateChild(@AuthenticationPrincipal Long teacherId,
                                     @PathVariable Long childId,
                                     @Valid @RequestBody ChildRequest request) {
        return childService.updateChild(teacherId, childId, request);
    }

    @DeleteMapping("/children/{childId}")
    public ResponseEntity<Void> deleteChild(@AuthenticationPrincipal Long teacherId,
                                            @PathVariable Long childId) {
        childService.deleteChild(teacherId, childId);
        return ResponseEntity.noContent().build();
    }
}

package com.jaram.child;

import com.jaram.child.domain.Child;
import com.jaram.child.dto.ChildRequest;
import com.jaram.child.dto.ChildResponse;
import com.jaram.classroom.ClassroomRepository;
import com.jaram.common.exception.BusinessException;
import com.jaram.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ClassroomRepository classroomRepository;

    public ChildService(ChildRepository childRepository, ClassroomRepository classroomRepository) {
        this.childRepository = childRepository;
        this.classroomRepository = classroomRepository;
    }

    /** 반 명단(가나다순, 삭제 제외). 반 소유권 검증. */
    public List<ChildResponse> getChildren(Long teacherId, Long classroomId) {
        verifyClassroomOwnership(teacherId, classroomId);
        return childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId).stream()
                .map(ChildResponse::from)
                .toList();
    }

    @Transactional
    public ChildResponse registerChild(Long teacherId, Long classroomId, ChildRequest request) {
        verifyClassroomOwnership(teacherId, classroomId);
        String tokenAlias = generateTokenAlias(classroomId);
        Child child = childRepository.save(
                Child.create(classroomId, request.name(), request.birthDate(), tokenAlias));
        return ChildResponse.from(child);
    }

    @Transactional
    public ChildResponse updateChild(Long teacherId, Long childId, ChildRequest request) {
        Child child = findOwnedChild(teacherId, childId);
        child.update(request.name(), request.birthDate());
        return ChildResponse.from(child);
    }

    @Transactional
    public void deleteChild(Long teacherId, Long childId) {
        Child child = findOwnedChild(teacherId, childId);
        child.softDelete();
    }

    private void verifyClassroomOwnership(Long teacherId, Long classroomId) {
        classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));
    }

    /** 아이 + 소유권을 함께 검증. 미존재/삭제/타 교사 소유는 모두 CHILD_NOT_FOUND(존재 비노출). */
    private Child findOwnedChild(Long teacherId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        classroomRepository.findByIdAndTeacherId(child.getClassroomId(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        return child;
    }

    /**
     * 반 내 유일한 안정 가명 부여(예: 아이A, 아이B...). 삭제 아이도 alias 점유 → 전체 행 수+1 로 인덱스 산정.
     */
    private String generateTokenAlias(Long classroomId) {
        long next = childRepository.countAllIncludingDeleted(classroomId) + 1;
        return "아이" + toLetters(next);
    }

    /** 1→A, 26→Z, 27→AA ... (엑셀 열 방식). */
    private static String toLetters(long index) {
        StringBuilder sb = new StringBuilder();
        long n = index;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.toString();
    }
}

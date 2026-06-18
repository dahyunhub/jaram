package com.ondo.classroom;

import com.ondo.classroom.dto.ClassroomResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClassroomService {

    private final ClassroomRepository classroomRepository;

    public ClassroomService(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    /**
     * 현재 교사가 소유한 반만 반환(repository 레벨 소유권 강제). 타 교사 반은 조회되지 않는다.
     */
    public List<ClassroomResponse> getMyClassrooms(Long teacherId) {
        return classroomRepository.findClassroomSummaryRows(teacherId).stream()
                .map(ClassroomService::toResponse)
                .toList();
    }

    private static ClassroomResponse toResponse(Object[] row) {
        return new ClassroomResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).intValue(),
                toLocalDate(row[3]),
                ((Number) row[4]).longValue());
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}

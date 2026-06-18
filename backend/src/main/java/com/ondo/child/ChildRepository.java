package com.ondo.child;

import com.ondo.child.domain.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {

    /** 반 명단(가나다순, soft delete 제외 — @SQLRestriction). 동명이인은 id 오름차순으로 안정 정렬. */
    List<Child> findByClassroomIdOrderByNameAscIdAsc(Long classroomId);

    /**
     * token_alias 부여용. 삭제된 아이도 alias 를 점유하므로(UNIQUE) 네이티브로 전체 행을 센다(@SQLRestriction 우회).
     */
    @Query(value = "SELECT COUNT(*) FROM child WHERE classroom_id = :classroomId", nativeQuery = true)
    long countAllIncludingDeleted(@Param("classroomId") Long classroomId);
}

package com.ondo.child;

import com.ondo.child.domain.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {

    /** 반 명단(가나다순, soft delete 제외 — @SQLRestriction). 동명이인은 id 오름차순으로 안정 정렬. */
    List<Child> findByClassroomIdOrderByNameAscIdAsc(Long classroomId);

    /**
     * 숨긴 아이(soft delete) 명단 — 기록 참고용. @SQLRestriction(deleted_at IS NULL)을 우회해야 하므로 네이티브로 조회한다.
     * 가나다순(동명이인 id asc).
     */
    @Query(value = "SELECT * FROM child WHERE classroom_id = :classroomId AND deleted_at IS NOT NULL "
            + "ORDER BY name ASC, id ASC", nativeQuery = true)
    List<Child> findHiddenByClassroomId(@Param("classroomId") Long classroomId);

    /** 숨긴 아이 복원 시 소유권 검증용 — 삭제 여부 무관 조회(@SQLRestriction 우회). */
    @Query(value = "SELECT * FROM child WHERE id = :id", nativeQuery = true)
    java.util.Optional<Child> findByIdIncludingDeleted(@Param("id") Long id);

    /** 숨김 해제(복원) — deleted_at 을 비운다. @SQLRestriction 우회 + 영속성 컨텍스트 동기화. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE child SET deleted_at = NULL, updated_at = :now WHERE id = :id", nativeQuery = true)
    int restoreById(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    /**
     * token_alias 부여용. 삭제된 아이도 alias 를 점유하므로(UNIQUE) 네이티브로 전체 행을 센다(@SQLRestriction 우회).
     */
    @Query(value = "SELECT COUNT(*) FROM child WHERE classroom_id = :classroomId", nativeQuery = true)
    long countAllIncludingDeleted(@Param("classroomId") Long classroomId);
}

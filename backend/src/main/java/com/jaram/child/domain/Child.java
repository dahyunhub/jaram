package com.jaram.child.domain;

import com.jaram.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 아이(FR-11, NFR-1, NFR-2). name 은 실명(DB 에만 저장, 외부 AI 전송 금지).
 * token_alias 는 반 내 유일한 안정 가명(등록 시 부여). soft delete: deleted_at + @SQLRestriction.
 */
@Entity
@Table(name = "child")
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "token_alias", nullable = false)
    private String tokenAlias;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Child(Long classroomId, String name, LocalDate birthDate, Gender gender, String tokenAlias) {
        this.classroomId = classroomId;
        this.name = normalizeName(name);
        this.birthDate = birthDate;
        this.gender = gender;
        this.tokenAlias = tokenAlias;
    }

    public static Child create(Long classroomId, String name, LocalDate birthDate, Gender gender, String tokenAlias) {
        return new Child(classroomId, name, birthDate, gender, tokenAlias);
    }

    public void update(String name, LocalDate birthDate, Gender gender) {
        this.name = normalizeName(name);
        this.birthDate = birthDate;
        this.gender = gender;
    }

    /** 이름 앞뒤 공백 제거(정렬·표시 일관성). */
    private static String normalizeName(String name) {
        return name == null ? null : name.strip();
    }

    /** soft delete. 물리 삭제 대신 deleted_at 을 채워 기록을 보존한다(NFR-2). */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}

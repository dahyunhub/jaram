package com.jaram.classroom.domain;

import com.jaram.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 반(FR-10). 같은 이름이라도 학년도(year)로 구분한다. 소유 교사는 teacherId 로 식별(repository 레벨 인가).
 */
@Entity
@Table(name = "classroom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Classroom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private String name;

    @Column(name = "year", nullable = false)
    private Integer year;

    /** 학년도 시작일. FR-8 평가 기간 기본 시작점. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    private Classroom(Long teacherId, String name, Integer year, LocalDate startDate) {
        this.teacherId = teacherId;
        this.name = name;
        this.year = year;
        this.startDate = startDate;
    }

    public static Classroom create(Long teacherId, String name, Integer year, LocalDate startDate) {
        return new Classroom(teacherId, name, year, startDate);
    }
}

package com.ondo.child.dto;

import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 아이 응답(API [3]/[4]/[5]). photoUpdatedAt: 프로필 사진 갱신시각(없으면 null) — 프론트 아바타 표시·캐시 키.
 */
public record ChildResponse(
        Long id,
        String name,
        LocalDate birthDate,
        Gender gender,
        String tokenAlias,
        LocalDateTime photoUpdatedAt
) {

    public static ChildResponse from(Child child) {
        return from(child, null);
    }

    public static ChildResponse from(Child child, LocalDateTime photoUpdatedAt) {
        return new ChildResponse(child.getId(), child.getName(), child.getBirthDate(),
                child.getGender(), child.getTokenAlias(), photoUpdatedAt);
    }
}

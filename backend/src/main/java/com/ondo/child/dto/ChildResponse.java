package com.ondo.child.dto;

import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;

import java.time.LocalDate;

/**
 * 아이 응답(API [3]/[4]/[5]).
 */
public record ChildResponse(
        Long id,
        String name,
        LocalDate birthDate,
        Gender gender,
        String tokenAlias
) {

    public static ChildResponse from(Child child) {
        return new ChildResponse(child.getId(), child.getName(), child.getBirthDate(),
                child.getGender(), child.getTokenAlias());
    }
}

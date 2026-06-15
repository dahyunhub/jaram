package com.jaram.child.dto;

import com.jaram.child.domain.Child;

import java.time.LocalDate;

/**
 * 아이 응답(API [3]/[4]/[5]).
 */
public record ChildResponse(
        Long id,
        String name,
        LocalDate birthDate,
        String tokenAlias
) {

    public static ChildResponse from(Child child) {
        return new ChildResponse(child.getId(), child.getName(), child.getBirthDate(), child.getTokenAlias());
    }
}

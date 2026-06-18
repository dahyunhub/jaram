package com.ondo.memo.dto;

import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 메모 응답(API [7]). curriculumArea 는 미분류 시 null.
 * createdAt 은 UTC Instant(ISO-8601 'Z').
 */
public record MemoResponse(
        Long id,
        Long childId,
        String content,
        String playActivity,
        String interaction,
        String attitude,
        CurriculumArea curriculumArea,
        Instant createdAt
) {

    public static MemoResponse from(Memo m) {
        return new MemoResponse(m.getId(), m.getChildId(), m.getContent(),
                m.getPlayActivity(), m.getInteraction(), m.getAttitude(),
                m.getCurriculumArea(), m.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}

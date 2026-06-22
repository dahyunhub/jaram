package com.ondo.journal.domain;

/**
 * 일지 상태(data-model-spec §3). DRAFT=AI 생성 직후 초안, CONFIRMED=교사 확정(Story 3.6).
 */
public enum JournalStatus {
    DRAFT,
    CONFIRMED
}

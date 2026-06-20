package com.ondo.journal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 일지↔반영된 메모 추적 링크(추적성). 테이블 journal_memo_link 는 Flyway V1 정본(created_at 만, updated_at 없음).
 * UNIQUE(daily_journal_id, memo_id).
 */
@Entity
@Table(name = "journal_memo_link")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalMemoLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_journal_id", nullable = false)
    private Long dailyJournalId;

    @Column(name = "memo_id", nullable = false)
    private Long memoId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private JournalMemoLink(Long dailyJournalId, Long memoId) {
        this.dailyJournalId = dailyJournalId;
        this.memoId = memoId;
    }

    public static JournalMemoLink of(Long dailyJournalId, Long memoId) {
        return new JournalMemoLink(dailyJournalId, memoId);
    }
}

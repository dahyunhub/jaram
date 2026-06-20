package com.ondo.journal;

import com.ondo.journal.domain.DailyJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface DailyJournalRepository extends JpaRepository<DailyJournal, Long> {

    /** 일지 1건 유일성(UNIQUE teacher_id, classroom_id, journal_date) 검사. 이미 있으면 JOURNAL_ALREADY_EXISTS. */
    boolean existsByTeacherIdAndClassroomIdAndJournalDate(Long teacherId, Long classroomId, LocalDate journalDate);
}

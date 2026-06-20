package com.ondo.journal;

import com.ondo.journal.domain.JournalMemoLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalMemoLinkRepository extends JpaRepository<JournalMemoLink, Long> {
}

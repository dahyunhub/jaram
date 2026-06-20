package com.ondo.journal;

import com.ondo.journal.dto.JournalAnalyzeRequest;
import com.ondo.journal.dto.JournalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보육일지 API(Epic 3). 정본: api-spec §5.
 */
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    /** [11] 하루치 일지 초안 생성 — 201 DRAFT. */
    @PostMapping("/analyze")
    public ResponseEntity<JournalResponse> analyze(@AuthenticationPrincipal Long teacherId,
                                                   @Valid @RequestBody JournalAnalyzeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journalService.analyze(teacherId, request));
    }
}

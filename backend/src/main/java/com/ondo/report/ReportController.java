package com.ondo.report;

import com.ondo.report.dto.ReportResponse;
import com.ondo.report.dto.ReportSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 개인 관찰 평가 API(Epic 4). 정본: api-spec [16][17][18].
 */
@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** [16] 수동 평가 생성 — 201. 기간 자동 계산, type=MANUAL. */
    @PostMapping("/children/{childId}/reports")
    public ResponseEntity<ReportResponse> create(@AuthenticationPrincipal Long teacherId,
                                                 @PathVariable Long childId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.createManual(teacherId, childId));
    }

    /** [17] 평가 목록 — 200(시간순, content 생략). */
    @GetMapping("/children/{childId}/reports")
    public List<ReportSummaryResponse> list(@AuthenticationPrincipal Long teacherId,
                                            @PathVariable Long childId) {
        return reportService.list(teacherId, childId);
    }

    /** [18] 평가 단건 — 200(content 포함). */
    @GetMapping("/reports/{reportId}")
    public ReportResponse get(@AuthenticationPrincipal Long teacherId,
                              @PathVariable Long reportId) {
        return reportService.get(teacherId, reportId);
    }
}

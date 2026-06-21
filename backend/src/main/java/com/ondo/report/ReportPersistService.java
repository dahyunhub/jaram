package com.ondo.report;

import com.ondo.report.domain.ChildReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 평가 결과 저장 전담(REQUIRES_NEW). AI 호출이 끝난 뒤에만 짧게 TX 를 연다 — 오케스트레이션(ReportService)이
 * 주입받아 호출(self-invocation 회피). 일지와 달리 링크 테이블·메모 영역 갱신 없음(평가는 FR-2 분류 안 함).
 * 정본: ai-integration-spec §7.
 */
@Service
public class ReportPersistService {

    private final ChildReportRepository childReportRepository;

    public ReportPersistService(ChildReportRepository childReportRepository) {
        this.childReportRepository = childReportRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult saveManual(Long childId, LocalDate periodStart, LocalDate periodEnd, String contentJson) {
        ChildReport report = childReportRepository.save(
                ChildReport.createManual(childId, periodStart, periodEnd, contentJson));
        return new PersistResult(report.getId(), report.getCreatedAt());
    }

    public record PersistResult(Long reportId, LocalDateTime createdAt) {
    }
}

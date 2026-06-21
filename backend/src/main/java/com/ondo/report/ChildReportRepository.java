package com.ondo.report;

import com.ondo.report.domain.ChildReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChildReportRepository extends JpaRepository<ChildReport, Long> {

    /** 평가 목록([17]): 생성 시간순. 수동·자동 모두 포함. */
    List<ChildReport> findByChildIdOrderByCreatedAtAsc(Long childId);

    /** 기간 계산용: 직전 평가(period_end 최댓값). 없으면 empty → 반 start_date 사용. */
    Optional<ChildReport> findTopByChildIdOrderByPeriodEndDesc(Long childId);
}

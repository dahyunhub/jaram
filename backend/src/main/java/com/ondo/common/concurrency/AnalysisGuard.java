package com.ondo.common.concurrency;

import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 사용자당 동시 분석 1건 가드(NFR-4 비용·NFR-3 커넥션). 단일 컨테이너 in-memory 전제.
 * 오케스트레이션(JournalService/ReportService) 진입점에서 {@link #runExclusively} 로 감싼다.
 * 점유·해제가 단일 호출 안에 갇혀(acquire/release 비공개) 해제 누수·타 요청 락 오해제가 구조적으로 불가능하다.
 * 인프라(OpenAiClient)·도메인 서비스를 모른다(common 계층 독립, 의존성 방향 고정).
 * 클러스터(다중 인스턴스) 분산락은 post-MVP. 정본: docs/specs/ai-integration-spec.md §7.
 */
@Component
public class AnalysisGuard {

    /** 진행 중인 사용자 id 집합. add()가 원자적으로 점유 여부를 판정한다. */
    private final Set<Long> inProgress = ConcurrentHashMap.newKeySet();

    /** 진행 중이면 ANALYSIS_IN_PROGRESS(409). 아니면 점유한다. runExclusively 전용(비공개). */
    private void acquire(Long userId) {
        if (!inProgress.add(userId)) {
            throw new BusinessException(ErrorCode.ANALYSIS_IN_PROGRESS);
        }
    }

    /** 점유 해제. 자신이 점유한 호출의 finally 에서만 호출된다(비공개 — 타 요청 락 오해제 방지). */
    private void release(Long userId) {
        inProgress.remove(userId);
    }

    /** 사용자당 1건 배타 실행 — acquire → 실행 → finally release 캡슐화. 진행 중이면 ANALYSIS_IN_PROGRESS. */
    public <T> T runExclusively(Long userId, Supplier<T> action) {
        acquire(userId);
        try {
            return action.get();
        } finally {
            release(userId);
        }
    }

    /** 반환값 없는 배타 실행. */
    public void runExclusively(Long userId, Runnable action) {
        runExclusively(userId, () -> {
            action.run();
            return null;
        });
    }
}

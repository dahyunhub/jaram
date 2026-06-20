package com.ondo.common.concurrency;

import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AnalysisGuard 순수 단위 테스트(스프링 컨텍스트·DB 불필요). 공개 API 는 runExclusively 뿐. */
class AnalysisGuardTest {

    private final AnalysisGuard guard = new AnalysisGuard();

    @Test
    void 정상_종료시_값을_반환하고_해제한다() {
        String result = guard.runExclusively(1L, () -> "ok");

        assertThat(result).isEqualTo("ok");
        // 해제됐으므로 재실행 가능
        assertThatCode(() -> guard.runExclusively(1L, () -> "again")).doesNotThrowAnyException();
    }

    @Test
    void 진행중_같은_사용자_재진입은_거절된다() {
        assertThatThrownBy(() -> guard.runExclusively(1L, () ->
                guard.runExclusively(1L, () -> "nested")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_IN_PROGRESS);
    }

    @Test
    void 서로_다른_사용자는_동시에_점유할_수_있다() {
        String result = guard.runExclusively(1L, () ->
                guard.runExclusively(2L, () -> "both"));

        assertThat(result).isEqualTo("both");
    }

    @Test
    void 예외가_나도_해제를_보장한다() {
        assertThatThrownBy(() -> guard.runExclusively(1L, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        // finally 에서 해제됐으므로 다시 점유 가능
        assertThatCode(() -> guard.runExclusively(1L, () -> "after")).doesNotThrowAnyException();
    }

    @Test
    void 같은_사용자_동시_진입은_하나만_성공하고_나머지는_거절된다() throws InterruptedException {
        CountDownLatch holding = new CountDownLatch(1);   // t1 이 임계구역 진입 신호
        CountDownLatch proceed = new CountDownLatch(1);   // 메인이 검증을 마칠 때까지 t1 대기

        Thread t1 = new Thread(() -> guard.runExclusively(1L, () -> {
            holding.countDown();
            await(proceed);
            return null;
        }));
        t1.start();

        assertThat(holding.await(2, TimeUnit.SECONDS)).isTrue(); // t1 이 락을 잡은 상태 보장

        // 같은 사용자로 동시 진입 시도 → 거절
        assertThatThrownBy(() -> guard.runExclusively(1L, () -> "concurrent"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_IN_PROGRESS);

        proceed.countDown();
        t1.join(2000);

        // t1 해제 후엔 다시 점유 가능
        assertThatCode(() -> guard.runExclusively(1L, () -> "ok")).doesNotThrowAnyException();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}

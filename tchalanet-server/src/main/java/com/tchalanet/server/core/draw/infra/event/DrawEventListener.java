package com.tchalanet.server.core.draw.infra.event;

import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.core.draw.domain.event.DrawCancelledEvent;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.infra.cache.DrawCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrawEventListener {

    private static final String KEY_DRAW_RESULT_APPLIED_CACHE_EVICT =
        "draw.cache.evict.result_applied";
    private static final String KEY_DRAW_RESULT_CORRECTED_CACHE_EVICT =
        "draw.cache.evict.result_corrected";
    private static final String KEY_DRAW_SETTLED_CACHE_EVICT =
        "draw.cache.evict.settled";
    private static final String KEY_DRAW_CANCELLED_CACHE_EVICT =
        "draw.cache.evict.cancelled";

    private final ProcessedEventPort processedEventPort;
    private final DrawCacheEvictor drawCacheEvictor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultApplied(DrawResultAppliedEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(
            KEY_DRAW_RESULT_APPLIED_CACHE_EVICT,
            event.eventId().value())) {
            return;
        }

        drawCacheEvictor.evictAll();

        log.debug(
            "draw.cache.evicted reason=DrawResultApplied tenantId={} drawId={} resultSlotId={}",
            event.tenantId(),
            event.drawId(),
            event.resultSlotId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(
            KEY_DRAW_RESULT_CORRECTED_CACHE_EVICT,
            event.eventId().value())) {
            return;
        }

        drawCacheEvictor.evictAll();

        log.debug(
            "draw.cache.evicted reason=DrawResultCorrected tenantId={} drawId={} resultSlotId={}",
            event.tenantId(),
            event.drawId(),
            event.resultSlotId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawSettled(DrawSettledEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(
            KEY_DRAW_SETTLED_CACHE_EVICT,
            event.eventId().value())) {
            return;
        }

        drawCacheEvictor.evictAll();

        log.debug(
            "draw.cache.evicted reason=DrawSettled tenantId={} drawId={} resultSlotId={}",
            event.tenantId(),
            event.drawId(),
            event.resultSlotId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawCancelled(DrawCancelledEvent event) {
        if (!processedEventPort.markProcessedIfAbsent(
            KEY_DRAW_CANCELLED_CACHE_EVICT,
            event.eventId().value())) {
            return;
        }

        drawCacheEvictor.evictAll();

        log.debug(
            "draw.cache.evicted reason=DrawCancelled tenantId={} drawId={}",
            event.tenantId(),
            event.drawId());
    }
}

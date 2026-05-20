package com.tchalanet.server.core.draw.internal.infra.event;

import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.core.draw.api.event.DrawCancelledEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.draw.api.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.internal.infra.cache.DrawCacheEvictor;
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

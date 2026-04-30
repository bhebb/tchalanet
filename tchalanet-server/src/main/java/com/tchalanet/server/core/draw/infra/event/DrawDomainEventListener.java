package com.tchalanet.server.core.draw.infra.event;

import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent;
import com.tchalanet.server.core.drawresult.infra.cache.DrawResultCacheEvictor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class DrawDomainEventListener {

  private static final String KEY_CACHE_INVALIDATE = "draw.event.cache_invalidate";
  private static final String KEY_SETTLED_NOTIFY = "draw.event.settled_notify";

  private final ProcessedEventPort processedEventPort;
  private final DrawResultCacheEvictor cacheEvictor;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDrawResultIngested(DrawResultIngestedEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(KEY_CACHE_INVALIDATE, event.eventId().value())) return;

    log.info(
        "DrawResultIngestedEvent received: tenantId={}, drawId={}, resultSlotId={}, drawDate={}",
        event.tenantId(),
        event.drawId(),
        event.resultSlotId(),
        event.drawDate());

    cacheEvictor.evictAll();
    log.debug("draw.cache.evicted reason=DrawResultIngested drawId={}", event.drawId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDrawSettled(DrawSettledEvent event) {
    if (!processedEventPort.markProcessedIfAbsent(KEY_SETTLED_NOTIFY, event.eventId().value())) return;

    log.info(
        "DrawSettledEvent received: tenantId={}, drawId={}, gameCode={}, drawChannelCode={}",
        event.tenantId(),
        event.drawId(),
        event.gameCode(),
        event.channelCode());

    cacheEvictor.evictAll();
    log.debug("draw.cache.evicted reason=DrawSettled drawId={}", event.drawId());
  }
}

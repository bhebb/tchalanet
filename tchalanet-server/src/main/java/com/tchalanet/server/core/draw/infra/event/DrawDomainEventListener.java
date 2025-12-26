package com.tchalanet.server.core.draw.infra.event;

import com.tchalanet.server.core.draw.domain.event.DrawResultedEvent;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DrawDomainEventListener {

  @EventListener
  public void onDrawResulted(DrawResultedEvent event) {
    log.info(
        "DrawResultedEvent received: tenantId={}, drawId={}, gameCode={}, channelCode={}",
        event.tenantId(),
        event.drawId(),
        event.gameCode(),
        event.channelCode());
    // TODO: invalidate caches for today/last-days/next draws
  }

  @EventListener
  public void onDrawSettled(DrawSettledEvent event) {
    log.info(
        "DrawSettledEvent received: tenantId={}, drawId={}, gameCode={}, channelCode={}",
        event.tenantId(),
        event.drawId(),
        event.gameCode(),
        event.channelCode());
    // TODO: invalidate caches for today/last-days/next draws
  }
}

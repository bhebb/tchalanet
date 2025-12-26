package com.tchalanet.server.core.session.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.payout.infra.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.session.application.command.model.RecomputePosSessionTotalsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PosSessionTotalsProjectionListener {

  private final CommandBus commandBus;

  @EventListener
  public void onTicketPlaced(com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputePosSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }

  @EventListener
  public void onTicketCancelled(
      com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputePosSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }

  @EventListener
  public void onPayoutRegistered(PayoutRegisteredEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputePosSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }
}

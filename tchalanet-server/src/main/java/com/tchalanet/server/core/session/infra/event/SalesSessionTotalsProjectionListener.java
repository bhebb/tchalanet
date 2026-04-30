package com.tchalanet.server.core.session.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.payout.infra.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.session.application.command.model.RecomputeSalesSessionTotalsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesSessionTotalsProjectionListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onTicketPlaced(com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputeSalesSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onTicketCancelled(
      com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputeSalesSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onPayoutRegistered(PayoutRegisteredEvent e) {
    if (e.sessionId() == null) return;
    commandBus.send(new RecomputeSalesSessionTotalsCommand(e.tenantId(), e.sessionId()));
  }
}

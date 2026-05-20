package com.tchalanet.server.core.limitpolicy.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.limitpolicy.api.command.ApplyTicketExposureCommand;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class LimitPolicyEventsListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void on(TicketPlacedEvent e) {
    commandBus.execute(new ApplyTicketExposureCommand(e));
  }
}

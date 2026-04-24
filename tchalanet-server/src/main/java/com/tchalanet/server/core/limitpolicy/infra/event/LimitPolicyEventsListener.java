package com.tchalanet.server.core.limitpolicy.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.limitpolicy.application.command.model.ApplyTicketExposureCommand;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
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
    commandBus.send(new ApplyTicketExposureCommand(e));
  }
}

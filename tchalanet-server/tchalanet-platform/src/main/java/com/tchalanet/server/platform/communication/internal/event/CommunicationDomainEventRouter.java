package com.tchalanet.server.platform.communication.internal.event;

import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.internal.rule.CommunicationRule;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommunicationDomainEventRouter {

  private final List<CommunicationRule<Object>> rules;
  private final CommunicationApi communicationApi;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void route(Object event) {
    for (var rule : rules) {
      if (!rule.supports(event)) {
        continue;
      }

      rule.map(event).ifPresent(communicationApi::enqueue);
      log.debug("Communication rule evaluated rule={} event={}",
          rule.getClass().getSimpleName(), event.getClass().getSimpleName());
    }
  }
}

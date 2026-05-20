package com.tchalanet.server.platform.communication.internal.rule;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SalesCommunicationRule extends AbstractCommunicationRule<Object> {

  @Override
  public boolean supports(Object event) {
    return event != null && event.getClass().getSimpleName().startsWith("Ticket");
  }

  @Override
  public Optional<SendOutboundMessageRequest> map(Object event) {
    return Optional.empty();
  }
}

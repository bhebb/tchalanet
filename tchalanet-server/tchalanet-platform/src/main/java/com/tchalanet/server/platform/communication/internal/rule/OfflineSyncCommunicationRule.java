package com.tchalanet.server.platform.communication.internal.rule;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OfflineSyncCommunicationRule extends AbstractCommunicationRule<Object> {

  @Override
  public boolean supports(Object event) {
    return event != null && event.getClass().getSimpleName().startsWith("Offline");
  }

  @Override
  public Optional<SendOutboundMessageRequest> map(Object event) {
    return Optional.empty();
  }
}

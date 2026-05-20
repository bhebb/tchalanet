package com.tchalanet.server.platform.communication.internal.rule;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import java.util.Optional;

public interface CommunicationRule<T> {

  boolean supports(Object event);

  Optional<SendOutboundMessageRequest> map(T event);
}

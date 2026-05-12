package com.tchalanet.server.platform.communication.api.model.request;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import java.util.Locale;
import java.util.Map;

public record SendOutboundMessageRequest(
    String type,
    CommunicationChannel channel,
    OutboundRecipient recipient,
    Locale locale,
    Map<String, Object> metadata) {

  public SendOutboundMessageRequest {
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}

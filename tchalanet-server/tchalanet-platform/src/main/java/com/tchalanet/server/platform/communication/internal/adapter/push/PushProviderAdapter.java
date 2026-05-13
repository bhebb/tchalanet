package com.tchalanet.server.platform.communication.internal.adapter.push;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProvider;
import org.springframework.stereotype.Component;

@Component
public class PushProviderAdapter implements DeliveryProvider {

  @Override
  public boolean supports(CommunicationChannel channel) {
    return channel == CommunicationChannel.PUSH;
  }

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    return SendOutboundMessageResult.skipped("dev-push", "push not configured");
  }
}

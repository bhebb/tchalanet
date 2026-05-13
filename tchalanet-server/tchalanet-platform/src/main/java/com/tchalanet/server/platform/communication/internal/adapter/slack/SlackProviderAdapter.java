package com.tchalanet.server.platform.communication.internal.adapter.slack;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProvider;
import com.tchalanet.server.platform.communication.internal.provider.EdgeCommunicationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlackProviderAdapter implements DeliveryProvider {

  private final EdgeCommunicationGateway edgeCommunicationGateway;

  @Override
  public boolean supports(CommunicationChannel channel) {
    return channel == CommunicationChannel.SLACK
        || channel == CommunicationChannel.SLACK_INTERNAL
        || channel == CommunicationChannel.SLACK_TENANT_WEBHOOK;
  }

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    return edgeCommunicationGateway.send(request);
  }
}

package com.tchalanet.server.platform.communication.internal.adapter.email;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.internal.adapter.DeliveryProvider;
import com.tchalanet.server.platform.communication.internal.provider.EdgeCommunicationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/// Forwards EMAIL deliveries to the edge service (mirrors SlackProviderAdapter).
/// The edge service performs the actual send via its configured email provider
/// (EMAIL_ENABLED / EMAIL_PROVIDER, e.g. brevo).
@Component
@RequiredArgsConstructor
public class EmailProviderAdapter implements DeliveryProvider {

  private final EdgeCommunicationGateway edgeCommunicationGateway;

  @Override
  public boolean supports(CommunicationChannel channel) {
    return channel == CommunicationChannel.EMAIL;
  }

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    return edgeCommunicationGateway.send(request);
  }
}

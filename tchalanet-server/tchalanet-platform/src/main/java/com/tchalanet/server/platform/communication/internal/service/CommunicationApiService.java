package com.tchalanet.server.platform.communication.internal.service;

import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.internal.provider.EdgeCommunicationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class CommunicationApiService implements CommunicationApi {

  private final EdgeCommunicationGateway edgeCommunicationGateway;

  @Override
  public SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    return edgeCommunicationGateway.send(request);
  }
}

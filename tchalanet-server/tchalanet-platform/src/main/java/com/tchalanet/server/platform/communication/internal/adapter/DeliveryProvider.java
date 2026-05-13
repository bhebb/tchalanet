package com.tchalanet.server.platform.communication.internal.adapter;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;

public interface DeliveryProvider {

  boolean supports(CommunicationChannel channel);

  SendOutboundMessageResult send(SendOutboundMessageRequest request);
}

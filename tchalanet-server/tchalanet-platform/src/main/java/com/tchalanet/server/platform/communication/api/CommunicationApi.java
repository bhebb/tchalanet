package com.tchalanet.server.platform.communication.api;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;

public interface CommunicationApi {

  SendOutboundMessageResult send(SendOutboundMessageRequest request);
}

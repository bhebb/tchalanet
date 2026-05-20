package com.tchalanet.server.platform.communication.api;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.MessageId;

public interface CommunicationApi {

    MessageId enqueue(SendOutboundMessageRequest request);

    SendOutboundMessageResult sendNow(SendOutboundMessageRequest request);
}

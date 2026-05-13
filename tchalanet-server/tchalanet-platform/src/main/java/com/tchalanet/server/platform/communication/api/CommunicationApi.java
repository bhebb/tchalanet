package com.tchalanet.server.platform.communication.api;

import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.result.SendOutboundMessageResult;
import com.tchalanet.server.platform.communication.api.model.value.MessageId;

public interface CommunicationApi {

  MessageId enqueue(SendOutboundMessageRequest request);

  SendOutboundMessageResult sendNow(SendOutboundMessageRequest request);

  /**
   * Compatibility bridge while older callers migrate to {@link #enqueue(SendOutboundMessageRequest)}.
   */
  @Deprecated(forRemoval = false, since = "introduce-platform-communication")
  default SendOutboundMessageResult send(SendOutboundMessageRequest request) {
    var messageId = enqueue(request);
    return SendOutboundMessageResult.queued("platform.communication", messageId.value().toString());
  }
}

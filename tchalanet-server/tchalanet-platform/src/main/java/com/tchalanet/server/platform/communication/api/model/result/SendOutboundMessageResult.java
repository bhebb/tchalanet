package com.tchalanet.server.platform.communication.api.model.result;

public record SendOutboundMessageResult(
    boolean sent, String provider, String reason, String providerMessageId) {

  public static SendOutboundMessageResult queued(String provider, String messageId) {
    return new SendOutboundMessageResult(false, provider, "queued:" + messageId, messageId);
  }

  public static SendOutboundMessageResult sent(String provider) {
    return sent(provider, null);
  }

  public static SendOutboundMessageResult sent(String provider, String providerMessageId) {
    return new SendOutboundMessageResult(true, provider, null, providerMessageId);
  }

  public static SendOutboundMessageResult skipped(String provider, String reason) {
    return new SendOutboundMessageResult(false, provider, reason, null);
  }
}

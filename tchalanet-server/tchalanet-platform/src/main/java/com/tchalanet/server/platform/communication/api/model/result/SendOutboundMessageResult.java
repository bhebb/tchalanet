package com.tchalanet.server.platform.communication.api.model.result;

public record SendOutboundMessageResult(boolean sent, String provider, String reason) {

  public static SendOutboundMessageResult sent(String provider) {
    return new SendOutboundMessageResult(true, provider, null);
  }

  public static SendOutboundMessageResult skipped(String provider, String reason) {
    return new SendOutboundMessageResult(false, provider, reason);
  }
}

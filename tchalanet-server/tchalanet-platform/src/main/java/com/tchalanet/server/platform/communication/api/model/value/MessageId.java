package com.tchalanet.server.platform.communication.api.model.value;

import java.util.UUID;

public record MessageId(UUID value) {

  public MessageId {
    if (value == null) {
      throw new IllegalArgumentException("message id is required");
    }
  }

  public static MessageId of(UUID value) {
    return new MessageId(value);
  }
}

package com.tchalanet.server.common.communication.api;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Generic request for sending an external message through the edge service. */
public record OutboundMessageRequest(
    String type,
    CommunicationChannel channel,
    OutboundRecipient recipient,
    Locale locale,
    Map<String, Object> metadata
) {
    public OutboundMessageRequest {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(channel, "channel is required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}

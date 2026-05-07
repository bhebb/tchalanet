package com.tchalanet.server.common.communication.api;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

/** Recipient metadata for an outbound external message. */
public record OutboundRecipient(
    TenantId tenantId,
    UserId userId,
    String to,
    String channelKey
) {
    public static OutboundRecipient slack(String channelKey) {
        return new OutboundRecipient(null, null, null, channelKey);
    }

    public static OutboundRecipient addressed(String to) {
        return new OutboundRecipient(null, null, to, null);
    }
}

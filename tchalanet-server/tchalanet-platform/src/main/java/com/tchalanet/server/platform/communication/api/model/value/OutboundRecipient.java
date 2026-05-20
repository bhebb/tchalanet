package com.tchalanet.server.platform.communication.api.model.value;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record OutboundRecipient(TenantId tenantId, UserId userId, String to, String channelKey) {

    public static OutboundRecipient slack(String channelKey) {
        return new OutboundRecipient(null, null, null, channelKey);
    }

    public static OutboundRecipient of(String to) {
        return new OutboundRecipient(null, null, to, null);
    }
}

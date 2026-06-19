package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;

public record TicketContextPayload(
    DrawId drawId,
    DrawChannelId drawChannelId
) {
}

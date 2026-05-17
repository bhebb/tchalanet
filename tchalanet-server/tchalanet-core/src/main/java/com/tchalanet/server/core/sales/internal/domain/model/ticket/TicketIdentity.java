package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;

public record TicketIdentity(
    TicketId id,
    TenantId tenantId
) {
    public TicketIdentity bumpVersion() {
        return new TicketIdentity(id, tenantId);
    }
}

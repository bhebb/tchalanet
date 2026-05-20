package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.view.TicketForPayoutView;
import jakarta.validation.constraints.NotNull;

public record GetTicketForPayoutQuery(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId
) implements Query<TicketForPayoutView> {}

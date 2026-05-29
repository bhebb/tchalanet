package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotNull;

public record OpenPayoutClaimFromSettlementCommand(
    @NotNull EventId sourceEventId,
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull DrawId drawId,
    @NotNull Long amountCents,
    @NotNull String currency,
    OutletId sellingOutletId,
    SalesSessionId sellingSessionId
) implements Command<OpenPayoutClaimResult> {}

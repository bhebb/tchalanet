package com.tchalanet.server.core.sales.api.command.payout;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MarkTicketPayoutPaidCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull UserId paidBy,
    @NotNull Instant paidAt
) implements Command<MarkTicketPayoutPaidResult> {}

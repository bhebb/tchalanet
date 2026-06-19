package com.tchalanet.server.core.sales.api.command.payout;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.util.Objects;

public record MarkTicketPayoutReversedCommand(
    TenantId tenantId,
    TicketId ticketId,
    UserId reversedBy,
    Instant reversedAt
) implements Command<MarkTicketPayoutReversedResult> {

    public MarkTicketPayoutReversedCommand {
        Objects.requireNonNull(tenantId,   "tenantId");
        Objects.requireNonNull(ticketId,   "ticketId");
        Objects.requireNonNull(reversedBy, "reversedBy");
        Objects.requireNonNull(reversedAt, "reversedAt");
    }
}

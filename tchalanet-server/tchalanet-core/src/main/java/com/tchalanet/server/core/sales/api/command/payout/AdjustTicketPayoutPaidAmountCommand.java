package com.tchalanet.server.core.sales.api.command.payout;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record AdjustTicketPayoutPaidAmountCommand(
    @NotNull TenantId tenantId,
    @NotNull TicketId ticketId,
    @NotNull BigDecimal previousPaidAmount,
    @NotNull BigDecimal adjustedPaidAmount,
    @NotBlank String reason,
    @NotNull UserId adjustedBy,
    @NotNull Instant adjustedAt)
    implements Command<AdjustTicketPayoutPaidAmountResult> {}

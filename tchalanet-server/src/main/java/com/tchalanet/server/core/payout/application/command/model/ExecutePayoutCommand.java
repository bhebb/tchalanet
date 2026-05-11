package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;
public record ExecutePayoutCommand(
    @NotNull TenantId tenantId,
    @NotNull PayoutId payoutId,
    @NotNull UserId paidBy,
    @NotNull SalesSessionId payingSessionId,
    @NotNull OutletId payingOutletId,
    TerminalId terminalId,
    String reason
) implements Command<PayoutWorkflowResult> {}

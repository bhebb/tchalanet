package com.tchalanet.server.core.session.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record OpenSalesSessionCommand(
    @NotNull TenantId tenantId,
    @NotNull OutletId outletId,
    @NotNull TerminalId terminalId,
    @NotNull UserId openedBy, long openingFloatCents) implements Command<OpenSalesSessionResult> {}

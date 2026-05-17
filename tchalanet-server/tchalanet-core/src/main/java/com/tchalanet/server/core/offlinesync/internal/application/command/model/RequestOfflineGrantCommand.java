package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record RequestOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull OutletId outletId,
    @NotNull UserId requestedBy,
    @NotNull String deviceProof
) implements Command<OfflineSalesGrantId> {}

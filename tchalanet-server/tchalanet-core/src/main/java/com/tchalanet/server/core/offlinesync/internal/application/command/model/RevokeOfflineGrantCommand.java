package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record RevokeOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineSalesGrantId grantId,
    @NotNull UserId revokedBy,
    String reason
) implements Command<Void> {}

package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevokeOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineGrantId grantId,
    @NotBlank String reason
) implements Command<Void> {}

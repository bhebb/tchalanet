package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/** Renews an existing grant — server may extend windows or rotate the code batch. */
public record RenewOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineGrantId currentGrantId
) implements Command<RequestOfflineGrantResult> {}

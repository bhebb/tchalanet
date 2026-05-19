package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;

/** Background job-invoked: transitions a grant past {@code syncAcceptedUntil} to EXPIRED. */
public record ExpireOfflineGrantCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineGrantId grantId
) implements Command<Void> {}

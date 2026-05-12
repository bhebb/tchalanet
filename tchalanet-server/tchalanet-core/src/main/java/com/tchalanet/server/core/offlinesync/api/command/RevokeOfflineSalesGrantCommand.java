package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record RevokeOfflineSalesGrantCommand(
    TenantId tenantId,
    OfflineSalesGrantId grantId,
    UserId performedBy,
    String reason
) implements Command<RevokeOfflineSalesGrantResult> {}


package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

public record ExpireOfflineSalesGrantCommand(
    TenantId tenantId,
    OfflineSalesGrantId grantId,
    UserId performedBy
) implements Command<ExpireOfflineSalesGrantResult> {}


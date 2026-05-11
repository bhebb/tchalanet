package com.tchalanet.server.core.offlinesync.application.validation;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record OfflineBatchActorContext(
    TenantId tenantId,
    TerminalId terminalId,
    OutletId outletId,
    UserId sellerUserId,
    SalesSessionId salesSessionId
) {}


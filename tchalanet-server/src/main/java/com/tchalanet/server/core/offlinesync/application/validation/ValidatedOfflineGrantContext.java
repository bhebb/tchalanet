package com.tchalanet.server.core.offlinesync.application.validation;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record ValidatedOfflineGrantContext(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    String terminalCode,
    String outletName,
    Instant sessionOpenedAt
) {}


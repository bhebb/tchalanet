package com.tchalanet.server.core.offlinesync.internal.application.validation;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.domain.model.SalesSessionStatus;
import java.time.Instant;

public record ValidatedReceiveOfflineBatchContext(
    TenantId tenantId,
    UserId sellerUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    String terminalCode,
    String outletName,
    SalesSessionStatus sessionStatus,
    Instant sessionClosedAt,
    boolean sessionFinalized
) {}



package com.tchalanet.server.core.payout.application.validation;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record ValidatedPosPayoutContext(
    TenantId tenantId,
    UserId actorUserId,
    TerminalId terminalId,
    OutletId outletId,
    SalesSessionId salesSessionId,
    String terminalCode,
    String outletName,
    Instant sessionOpenedAt
) {}

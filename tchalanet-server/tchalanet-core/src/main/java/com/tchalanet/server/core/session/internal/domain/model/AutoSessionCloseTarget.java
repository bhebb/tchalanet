package com.tchalanet.server.core.session.internal.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record AutoSessionCloseTarget(
    TenantId tenantId,
    SalesSessionId sessionId,
    OutletId outletId,
    TerminalId terminalId,
    UserId closedBy,
    Instant closedAt,
    String reason) {}

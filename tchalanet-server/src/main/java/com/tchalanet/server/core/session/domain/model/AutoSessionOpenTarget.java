package com.tchalanet.server.core.session.domain.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;
import java.time.LocalDate;

public record AutoSessionOpenTarget(
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId openedBy,
    LocalDate businessDate,
    Instant openedAt,
    Long openingFloatCents) {}

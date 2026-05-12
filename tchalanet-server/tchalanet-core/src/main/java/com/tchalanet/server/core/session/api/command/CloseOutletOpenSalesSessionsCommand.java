package com.tchalanet.server.core.session.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;
import java.time.LocalDate;


public record CloseOutletOpenSalesSessionsCommand(
    TenantId tenantId,
    OutletId outletId,
    LocalDate businessDate,
    Instant closedAt,
    UserId closedBy,
    String reason
) implements Command<CloseOutletOpenSalesSessionsResult> {}

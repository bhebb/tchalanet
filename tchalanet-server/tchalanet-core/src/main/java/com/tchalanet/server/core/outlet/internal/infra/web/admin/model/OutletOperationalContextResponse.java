package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.domain.model.SalesCapability;
import java.time.Instant;

/** Aggregated operational snapshot for an outlet. */
public record OutletOperationalContextResponse(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    boolean dayClosed,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    String timezone,
    boolean autoOpenSession,
    boolean autoCloseSession,
    long userCount,
    long terminalCount,
    SalesCapability salesCapability) {}

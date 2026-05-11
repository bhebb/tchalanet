package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record OutletSummaryView(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    boolean dayClosed,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    String timezone,
    boolean autoSessionOpenEnabled,
    boolean autoSessionCloseEnabled
) {}

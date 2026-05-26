package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletKind;
import com.tchalanet.server.core.outlet.internal.domain.model.OutletStatus;
import java.time.Instant;

public record OutletSummaryResponse(
    OutletId id,
    TenantId tenantId,
    String name,
    String slug,
    OutletKind kind,
    String partnerRef,
    SalesZoneId zoneId,
    OutletStatus status,
    boolean dayClosed,
    boolean outletBlocked,
    String outletBlockReason,
    boolean salesBlocked,
    String salesBlockReason,
    Instant salesBlockedAt,
    String timezone,
    boolean autoSessionOpenEnabled,
    boolean autoSessionCloseEnabled) {}

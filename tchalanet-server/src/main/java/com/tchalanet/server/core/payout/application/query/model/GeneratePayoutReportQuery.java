package com.tchalanet.server.core.payout.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.nio.file.Path;
import java.time.Instant;

public record GeneratePayoutReportQuery(
    TenantId tenantId, Instant from, Instant to, OutletId outletId, String format)
    implements Query<Path> {}

package com.tchalanet.server.features.reporting.outletreport;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.nio.file.Path;
import java.time.LocalDate;

public record GenerateOutletReportQuery(
    TenantId tenantId,
    OutletId outletId,
    LocalDate from,
    LocalDate to
) implements Query<Path> {}

package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record GenerateOutletReportQuery(TenantId tenantId, OutletId outletId, LocalDate from, LocalDate to) implements Query<java.nio.file.Path> {}

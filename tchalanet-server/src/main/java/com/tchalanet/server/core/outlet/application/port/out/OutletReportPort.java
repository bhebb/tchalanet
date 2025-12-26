package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.nio.file.Path;
import java.time.LocalDate;

public interface OutletReportPort {
  Path generateDailyReport(TenantId tenantId, OutletId outletId, LocalDate date);
}


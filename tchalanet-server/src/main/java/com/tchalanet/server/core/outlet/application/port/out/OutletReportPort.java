package com.tchalanet.server.core.outlet.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import java.nio.file.Path;
import java.time.LocalDate;

public interface OutletReportPort {
  Path generateReport(OutletId outletId, LocalDate from, LocalDate to);
}

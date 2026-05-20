package com.tchalanet.server.features.reporting.outletreport;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class OutletReportExportService {

  public Path generate(TenantId tenantId, OutletId outletId, LocalDate from, LocalDate to) {
    try {
      var path = Files.createTempFile("outlet-report-", ".csv");
      var csv =
          "tenant_id,outlet_id,from,to%n%s,%s,%s,%s%n"
              .formatted(tenantId, outletId, from, to);
      Files.writeString(path, csv);
      return path;
    } catch (IOException e) {
      throw new IllegalStateException("failed to generate outlet report", e);
    }
  }
}

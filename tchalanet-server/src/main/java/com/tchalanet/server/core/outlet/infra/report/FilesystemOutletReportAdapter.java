package com.tchalanet.server.core.outlet.infra.report;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.port.out.OutletReaderPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletReportPort;
import com.tchalanet.server.core.outlet.application.port.out.SalesTicketAdminPort;
import com.tchalanet.server.core.outlet.application.port.out.SessionLookupPort;
import com.tchalanet.server.core.outlet.application.query.model.OutletDailySummary;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilesystemOutletReportAdapter implements OutletReportPort {

  private final SalesTicketAdminPort salesAdmin;
  private final SessionLookupPort sessionLookup;
  private final OutletReaderPort outletReader;

  @Override
  public Path generateDailyReport(TenantId tenantId, OutletId outletId, LocalDate date) {
    // Build summary
    var outlet = outletReader.getRequired(outletId, tenantId);
    var zone = ZoneId.of(outlet.timezone());
    var from = date.atStartOfDay(zone).toInstant();
    var to = date.plusDays(1).atStartOfDay(zone).toInstant();

    var stats = salesAdmin.getCloseStats(tenantId, outletId, from, to);
    List<?> sessions = sessionLookup.findSessionIds(tenantId, outletId, from, to);

    OutletDailySummary summary =
        new OutletDailySummary(
            date,
            stats.total(),
            stats.sold(),
            stats.voided(),
            stats.resultedWin(),
            stats.resultedLoss(),
            stats.paid(),
            sessions.size(),
            outlet.name(),
            outlet.salesBlocked());

    // Generate CSV file in temp
    try {
      Path tmp = Files.createTempFile("outlet-report-", ".csv");
      try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
        // header
        w.write(
            "date,outletName,salesBlocked,total,sold,voided,resultedWin,resultedLoss,paid,sessionCount\n");
        // data
        w.write(
            String.format(
                "%s,%s,%b,%d,%d,%d,%d,%d,%d,%d\n",
                date.format(DateTimeFormatter.ISO_DATE),
                escapeCsv(summary.outletName()),
                summary.salesBlocked(),
                summary.totalTickets(),
                summary.sold(),
                summary.voided(),
                summary.resultedWin(),
                summary.resultedLoss(),
                summary.paid(),
                summary.sessionCount()));
      }
      return tmp;
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate outlet report", e);
    }
  }

  private static String escapeCsv(String v) {
    if (v == null) return "";
    if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
      return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }
}

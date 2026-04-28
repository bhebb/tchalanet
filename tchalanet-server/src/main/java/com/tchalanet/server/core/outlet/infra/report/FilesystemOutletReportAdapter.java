package com.tchalanet.server.core.outlet.infra.report;

import com.tchalanet.server.common.types.id.OutletId;
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
import java.util.ArrayList;
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
  public Path generateReport(OutletId outletId, LocalDate fromDate, LocalDate toDate) {
    var outlet = outletReader.getRequired(outletId);
    var zone = ZoneId.of(outlet.timezone());
    LocalDate from = fromDate == null ? LocalDate.now(zone) : fromDate;
    LocalDate to = toDate == null ? from : toDate;
    if (to.isBefore(from)) {
      throw new IllegalArgumentException("to must be >= from");
    }

    List<OutletDailySummary> summaries = new ArrayList<>();
    LocalDate date = from;
    while (!date.isAfter(to)) {
      var fromInstant = date.atStartOfDay(zone).toInstant();
      var toInstant = date.plusDays(1).atStartOfDay(zone).toInstant();
      var stats = salesAdmin.getCloseStats(outletId, fromInstant, toInstant);
      List<?> sessions = sessionLookup.findSessionIds(outletId, fromInstant, toInstant);
      summaries.add(
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
              outlet.salesBlocked()));
      date = date.plusDays(1);
    }

    try {
      Path tmp = Files.createTempFile("outlet-report-", ".csv");
      try (BufferedWriter w = Files.newBufferedWriter(tmp)) {
        w.write(
            "date,outletName,salesBlocked,total,sold,voided,resultedWin,resultedLoss,paid,sessionCount\n");
        for (var summary : summaries) {
          w.write(
              String.format(
                  "%s,%s,%b,%d,%d,%d,%d,%d,%d,%d\n",
                  summary.date().format(DateTimeFormatter.ISO_DATE),
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

package com.tchalanet.server.features.reporting.salesreport;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/sales-by-period-and-game")
@RequiredArgsConstructor
public class GetSalesReportByPeriodAndGameController {

  private final GetSalesReportByPeriodAndGameHandler handler;
  private final TchRequestContextHolder contextHolder;
  private Clock cLock;

  @GetMapping
  public SalesReportResponse getSalesReport(
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "gameCode", required = false) String gameCode) {
    var today = LocalDate.now(cLock);
    var toDate = (to != null) ? to : today;
    var fromDate = (from != null) ? from : toDate.minusDays(6); // default 7 jours

    var query =
        new GetSalesReportByPeriodAndGameQuery(
            contextHolder.get().tenantUuid(), fromDate, toDate, gameCode);

    return handler.handle(query);
  }
}

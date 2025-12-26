package com.tchalanet.server.features.reporting.outletperformance;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/outlet-performance")
@RequiredArgsConstructor
public class GetOutletPerformanceReportController {

  private final GetOutletPerformanceReportHandler handler;
  private final TchRequestContextHolder contextHolder;
  private final Clock clock;

  @GetMapping
  public OutletPerformanceReportResponse getOutletPerformance(
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "gameCode", required = false) String gameCode) {
    var today = LocalDate.now(clock);
    var toDate = (to != null) ? to : today;
    var fromDate = (from != null) ? from : toDate.minusDays(6); // défaut : 7 jours

    var query =
        new GetOutletPerformanceReportQuery(
            contextHolder.get().tenantUuid(), fromDate, toDate, gameCode);

    return handler.handle(query);
  }
}

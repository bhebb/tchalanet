package com.tchalanet.server.features.reporting.salesreport;

import com.tchalanet.server.common.context.TchContextResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/reports/sales-by-period-and-game")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Platform • Reports")
public class GetSalesReportByPeriodAndGameController {

  private final SalesReportService service;
  private final TchContextResolver contextResolver;
  private final Clock cLock;

  @Operation(summary = "Get sales report by period and game (platform)")
  @GetMapping
  public SalesReportResponse getSalesReport(
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "gameCode", required = false) String gameCode) {
    var today = LocalDate.now(cLock);
    var toDate = (to != null) ? to : today;
    var fromDate = (from != null) ? from : toDate.minusDays(6); // default 7 jours

    var holder = contextResolver.currentOrNull();
    var tenantUuid = holder != null ? holder.tenantUuid() : null;

    var criteria = new SalesReportCriteria(tenantUuid, fromDate, toDate, gameCode);

    return service.getReport(criteria);
  }
}

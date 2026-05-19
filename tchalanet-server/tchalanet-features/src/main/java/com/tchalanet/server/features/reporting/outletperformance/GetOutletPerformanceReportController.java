package com.tchalanet.server.features.reporting.outletperformance;

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
@RequestMapping("/tenant/reports/outlet-performance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Platform • Reports")
public class GetOutletPerformanceReportController {

  private final OutletPerformanceReportService service;
  private final TchContextResolver contextResolver;
  private final Clock clock;

  @Operation(summary = "Get outlet performance report (platform)")
  @GetMapping
  public OutletPerformanceReportResponse getOutletPerformance(
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "gameCode", required = false) String gameCode) {
    var today = LocalDate.now(clock);
    var toDate = (to != null) ? to : today;
    var fromDate = (from != null) ? from : toDate.minusDays(6); // défaut : 7 jours

    var holder = contextResolver.currentOrNull();
    var tenantUuid = holder != null ? holder.tenantUuid() : null;

    var criteria = new OutletPerformanceReportCriteria(tenantUuid, fromDate, toDate, gameCode);

    return service.getReport(criteria);
  }
}

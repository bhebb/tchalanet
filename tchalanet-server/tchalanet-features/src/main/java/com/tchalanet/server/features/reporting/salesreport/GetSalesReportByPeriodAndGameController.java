package com.tchalanet.server.features.reporting.salesreport;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.reporting.ReportPeriodResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
  private final ReportPeriodResolver periodResolver;

  @Operation(summary = "Get sales report by period and game (platform)")
  @GetMapping
  public ApiResponse<SalesReportResponse> getSalesReport(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "gameCode", required = false) String gameCode) {
    var period = periodResolver.resolve(from, to, ctx.tenantZoneId());
    var criteria =
        new SalesReportCriteria(
            ctx.tenantIdRequired().value(), period.from(), period.to(), gameCode);
    return ApiResponse.success(service.getReport(criteria));
  }
}

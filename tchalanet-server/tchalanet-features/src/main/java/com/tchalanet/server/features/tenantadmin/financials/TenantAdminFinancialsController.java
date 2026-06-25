package com.tchalanet.server.features.tenantadmin.financials;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.analytics.api.model.TenantFinancialBreakdownView;
import com.tchalanet.server.core.analytics.api.query.GetTenantFinancialBreakdownQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tenant-admin financial analytics endpoints. */
@RestController
@RequestMapping("/admin/financials")
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Tenant Admin • Financials")
public class TenantAdminFinancialsController {

  private final QueryBus queryBus;

  @GetMapping("/breakdown")
  @Operation(summary = "Tenant financial breakdown by day, draw and seller terminal")
  public ApiResponse<TenantFinancialBreakdownView> breakdown(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "100") int drawLimit,
      @RequestParam(defaultValue = "100") int sellerTerminalLimit
  ) {
    ZoneId zoneId = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneId.systemDefault();
    LocalDate effectiveTo = to != null ? to : LocalDate.now(zoneId);
    LocalDate effectiveFrom = from != null ? from : effectiveTo;

    return ApiResponse.success(queryBus.ask(new GetTenantFinancialBreakdownQuery(
        ctx.effectiveTenantIdRequired(),
        effectiveFrom,
        effectiveTo,
        drawLimit,
        sellerTerminalLimit
    )));
  }
}

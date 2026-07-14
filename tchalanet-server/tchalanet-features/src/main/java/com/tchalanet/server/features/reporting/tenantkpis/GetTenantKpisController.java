package com.tchalanet.server.features.reporting.tenantkpis;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.reporting.ReportPeriod;
import com.tchalanet.server.features.reporting.ReportPeriodResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/reports/tenant-kpis")
@RequiredArgsConstructor
@Tag(name = "Platform • Reports")
public class GetTenantKpisController {

  private final GetTenantKpisService service;
  private final Clock clock;
  private final ReportPeriodResolver periodResolver;

  @Operation(summary = "Get tenant KPIs report (platform)")
  @GetMapping
  @PreAuthorize(
      "hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN') or hasAuthority('ACTOR_SELLER_TERMINAL')")
  public ApiResponse<KpisResponse> get(
      @CurrentContext TchRequestContext ctx,
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "range", required = false) KpisRange range) {

    var criteria = buildTenantKpisCriteria(ctx, range, from, to);

    return ApiResponse.success(service.getKpis(criteria));
  }

  private TenantKpisCriteria buildTenantKpisCriteria(
      TchRequestContext ctx, KpisRange range, LocalDate from, LocalDate to) {
    var zoneId = ctx.tenantZoneId() != null ? ctx.tenantZoneId() : ZoneId.systemDefault();
    var today = LocalDate.now(clock.withZone(zoneId));

    ReportPeriod period;

    if (range != null && range != KpisRange.CUSTOM) {
      switch (range) {
        case TODAY -> period = periodResolver.resolve(from, to, today, today);
        case WEEK -> period = periodResolver.resolve(from, to, today.minusDays(6), today);
        case MONTH ->
            period = periodResolver.resolve(from, to, today.minusMonths(1).plusDays(1), today);
        case YEAR ->
            period = periodResolver.resolve(from, to, today.minusYears(1).plusDays(1), today);
        default -> throw new IllegalArgumentException("Unsupported range: " + range);
      }
    } else {
      period = periodResolver.resolve(from, to, ctx.tenantZoneId());
    }

    return new TenantKpisCriteria(ctx.tenantIdRequired().value(), period.from(), period.to());
  }
}

package com.tchalanet.server.features.reporting.tenantkpis;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/reports/tenant-kpis")
@RequiredArgsConstructor
@Tag(name = "Platform • Reports")
public class GetTenantKpisController {

  private final TchContextResolver contextResolver;
  private final GetTenantKpisUseCase useCase;
  private final Clock clockPort;

  @Operation(summary = "Get tenant KPIs report (platform)")
  @GetMapping
  @RequiresPermission("reporting:view")
  public KpisResponse get(
      @RequestParam(name = "from", required = false) LocalDate from,
      @RequestParam(name = "to", required = false) LocalDate to,
      @RequestParam(name = "range", required = false) KpisRange range) {

    var query = buildGetTenantKpisQuery(range, from, to);

    return useCase.getKpis(query);
  }

  private GetTenantKpisQuery buildGetTenantKpisQuery(
      KpisRange range, LocalDate from, LocalDate to) {
    var today = LocalDate.now(clockPort);

    LocalDate fromDate;
    LocalDate toDate;

    if (range != null && range != KpisRange.CUSTOM) {
      switch (range) {
        case TODAY -> {
          toDate = today;
          fromDate = today;
        }
        case WEEK -> {
          toDate = today;
          fromDate = today.minusDays(6);
        }
        case MONTH -> {
          toDate = today;
          fromDate = today.minusMonths(1).plusDays(1);
        }
        case YEAR -> {
          toDate = today;
          fromDate = today.minusYears(1).plusDays(1);
        }
        default -> throw new IllegalArgumentException("Unsupported range: " + range);
      }
    } else {
      // mode custom ou aucun param → défaut = 7 jours
      toDate = (to != null) ? to : today;
      fromDate = (from != null) ? from : toDate.minusDays(6);
    }

    var holder = contextResolver.currentOrNull();
    var tenantUuid = holder != null ? holder.tenantUuid() : null;
    return new GetTenantKpisQuery(tenantUuid, fromDate, toDate);
  }
}

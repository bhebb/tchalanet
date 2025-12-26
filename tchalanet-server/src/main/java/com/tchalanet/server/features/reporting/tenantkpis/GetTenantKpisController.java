package com.tchalanet.server.features.reporting.tenantkpis;

import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/tenant-kpis")
@RequiredArgsConstructor
public class GetTenantKpisController {

  private final TchRequestContextHolder contextHolder;
  private final GetTenantKpisUseCase useCase;
  private final Clock clockPort;

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

    return new GetTenantKpisQuery(contextHolder.get().tenantUuid(), fromDate, toDate);
  }
}

package com.tchalanet.server.reporting.web;

import com.tchalanet.server.draw.domain.usecase.GetNextDrawUseCase;
import com.tchalanet.server.reporting.domain.usecase.GetTenantKpisUseCase;
import com.tchalanet.server.reporting.web.dto.DrawSummaryDto;
import com.tchalanet.server.reporting.web.dto.KpisDto;
import com.tchalanet.server.tenant.domain.usecase.GetTenantFeaturesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API pour les dashboards et KPIs. */
@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
public class DashboardController {

  private final GetTenantFeaturesUseCase getTenantFeaturesUseCase;
  private final GetTenantKpisUseCase getTenantKpisUseCase;
  private final GetNextDrawUseCase getNextDrawUseCase;

  @GetMapping("/console/kpis")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public ResponseEntity<KpisDto> getConsoleKpis(
      @RequestParam String tenant, @RequestParam String role) {

    KpisDto kpis = getTenantKpisUseCase.execute(tenant, role);
    return ResponseEntity.ok(kpis);
  }

  @GetMapping("/console/draws/next")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public ResponseEntity<DrawSummaryDto> getNextDraw(@RequestParam String tenant) {

    DrawSummaryDto nextDraw = getNextDrawUseCase.execute(tenant);
    return ResponseEntity.ok(nextDraw);
  }
}

package com.tchalanet.server.api;

import com.tchalanet.server.dto.DrawSummaryDto;
import com.tchalanet.server.dto.KpisDto;
import com.tchalanet.server.dto.TenantFeaturesDto;
import com.tchalanet.server.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboards")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/tenant/features")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public TenantFeaturesDto getFeatures(@RequestParam String tenant, @RequestParam String role) {
    return dashboardService.getFeatures(tenant, role);
  }

  @GetMapping("/console/kpis")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public KpisDto getKpis(@RequestParam String tenant, @RequestParam String role) {
    return dashboardService.getKpis(tenant, role);
  }

  @GetMapping("/console/draws/next")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public DrawSummaryDto getNextDraw(@RequestParam String tenant) {
    return dashboardService.getNextDraw(tenant);
  }
}

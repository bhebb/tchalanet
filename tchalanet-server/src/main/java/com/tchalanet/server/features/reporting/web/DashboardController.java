package com.tchalanet.server.features.reporting.web;

import com.tchalanet.server.core.draw.application.query.handler.GetNextDrawHandler;
import com.tchalanet.server.core.draw.application.query.model.GetNextDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.tenant.domain.usecase.GetTenantFeaturesUseCase;
import com.tchalanet.server.features.reporting.domain.usecase.GetTenantKpisUseCase;
import com.tchalanet.server.features.reporting.web.dto.KpisDto;
import java.time.ZonedDateTime;
import java.util.UUID;
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
  private final GetNextDrawHandler drawQueryHandler;

  @GetMapping("/console/kpis")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public ResponseEntity<KpisDto> getConsoleKpis(
      @RequestParam String tenant, @RequestParam String role) {

    KpisDto kpis = getTenantKpisUseCase.execute(tenant, role);
    return ResponseEntity.ok(kpis);
  }

  @GetMapping("/console/draws/next")
  @PreAuthorize("hasAuthority('SCOPE_console.api:read')")
  public ResponseEntity<Draw> getNextDraw(
      @RequestParam String tenant, @RequestParam String channelCode, @RequestParam String role) {

    // todo zoneDateTime optional and web mapper request- domaine
    var nextDraw =
        drawQueryHandler.handle(
            new GetNextDrawQuery(UUID.fromString(tenant), channelCode, ZonedDateTime.now()));

    return ResponseEntity.ok(nextDraw);
  }
}

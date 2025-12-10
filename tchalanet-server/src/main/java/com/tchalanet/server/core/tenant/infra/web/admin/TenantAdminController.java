package com.tchalanet.server.core.tenant.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.tenant.application.command.model.ArchiveTenantCommand;
import com.tchalanet.server.core.tenant.application.query.model.GenerateTenantPerPeriodReportQuery;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantDashboardStatsQuery;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping("/{id}/archive")
  public ResponseEntity<Void> archive(@PathVariable UUID id, @RequestBody Map<String,String> body) {
    String reason = body.getOrDefault("reason", "archived_by_admin");
    commandBus.send(new ArchiveTenantCommand(id, reason));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/monthly-report")
  public ResponseEntity<String> generateMonthlyReport(@PathVariable UUID id, @RequestParam("month") String month) {
    var ym = YearMonth.parse(month);
    var path = queryBus.send(new GenerateTenantPerPeriodReportQuery(id, ym));
    return ResponseEntity.ok(path.toString());
  }

  @GetMapping("/{id}/dashboard")
  public ResponseEntity<Map<String,Object>> dashboard(@PathVariable UUID id, @RequestParam(value="since", required=false) String since) {
    LocalDate s = since == null ? LocalDate.now().minusDays(7) : LocalDate.parse(since);
    var stats = queryBus.send(new GetTenantDashboardStatsQuery(id, s));
    return ResponseEntity.ok((Map<String,Object>)stats);
  }
}


package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.outlet.application.command.model.UpdateOutletConfigCommand;
import com.tchalanet.server.core.outlet.application.query.model.GetOutletDailySummaryQuery;
import com.tchalanet.server.core.outlet.application.query.model.GenerateOutletReportQuery;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/outlets")
@RequiredArgsConstructor
public class OutletAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping("/{id}/config")
  public ResponseEntity<Void> updateConfig(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId, @RequestBody Map<String,Object> config) {
    commandBus.send(new UpdateOutletConfigCommand(tenantId, id, config));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/daily-summary")
  public ResponseEntity<Map<String,Object>> dailySummary(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId, @RequestParam("date") String date) {
    var summary = queryBus.send(new GetOutletDailySummaryQuery(tenantId, id, LocalDate.parse(date)));
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/{id}/report")
  public ResponseEntity<String> generateReport(@PathVariable UUID id, @RequestHeader("X-Tenant-Id") UUID tenantId, @RequestParam("from") String from, @RequestParam("to") String to) {
    var path = queryBus.send(new GenerateOutletReportQuery(tenantId, id, LocalDate.parse(from), LocalDate.parse(to)));
    return ResponseEntity.ok(path.toString());
  }
}

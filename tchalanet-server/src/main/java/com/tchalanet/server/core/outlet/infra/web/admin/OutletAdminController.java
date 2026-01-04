package com.tchalanet.server.core.outlet.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.outlet.application.command.model.*;
import com.tchalanet.server.core.outlet.application.query.model.*;
import com.tchalanet.server.core.outlet.infra.web.model.CloseOutletDayRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/outlets")
@RequiredArgsConstructor
@Tags({@Tag(name = "Admin • Outlets")})
public class OutletAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "Patch outlet configuration (admin)")
  @PatchMapping("/{id}/config")
  public ResponseEntity<Void> updateConfig(
      @PathVariable OutletId id,
      @RequestHeader("X-Tenant-Id") TenantId tenantId,
      @RequestBody OutletConfigPatch patch) {
    commandBus.send(new UpdateOutletConfigCommand(tenantId, id, patch));
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Close outlet day (admin)")
  @PostMapping("/{id}/close-day")
  public ResponseEntity<Void> closeDay(
      @PathVariable OutletId id,
      @RequestHeader("X-Tenant-Id") TenantId tenantId,
      @Valid @RequestBody(required = false) CloseOutletDayRequest req) {
    CloseOutletDayRequest r = req == null ? CloseOutletDayRequest.empty() : req;

    CloseOutletDayPayload payload =
        new CloseOutletDayPayload(r.fromOrNow(), r.toOrNow(), r.modeOrDefault(), r.reason());

    commandBus.send(new CloseOutletDayCommand(tenantId, id, payload));
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Reopen outlet day (admin)")
  @PostMapping("/{id}/reopen-day")
  public ResponseEntity<Void> reopenDay(
      @PathVariable OutletId id, @RequestHeader("X-Tenant-Id") TenantId tenantId) {
    commandBus.send(new ReopenOutletDayCommand(tenantId, id, LocalDate.now()));
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Get outlet daily summary (admin)")
  @GetMapping("/{id}/daily-summary")
  public ResponseEntity<OutletDailySummary> dailySummary(
      @PathVariable OutletId id,
      @RequestHeader("X-Tenant-Id") TenantId tenantId,
      @RequestParam("date") String date) {
    var summary =
        queryBus.send(new GetOutletDailySummaryQuery(tenantId, id, LocalDate.parse(date)));
    return ResponseEntity.ok(summary);
  }

  @Operation(summary = "Generate outlet report (admin)")
  @GetMapping("/{id}/report")
  public ResponseEntity<String> generateReport(
      @PathVariable OutletId id,
      @RequestHeader("X-Tenant-Id") TenantId tenantId,
      @RequestParam("from") String from,
      @RequestParam("to") String to) {
    var path =
        queryBus.send(
            new GenerateOutletReportQuery(
                tenantId, id, LocalDate.parse(from), LocalDate.parse(to)));
    return ResponseEntity.ok(path.toString());
  }

  @Operation(summary = "Download outlet report (admin)")
  @GetMapping("/{id}/report/download")
  public ResponseEntity<Resource> downloadReport(
      @PathVariable OutletId id,
      @RequestHeader("X-Tenant-Id") TenantId tenantId,
      @RequestParam("date") String date) {
    LocalDate d;
    try {
      d = LocalDate.parse(date);
    } catch (DateTimeParseException e) {
      return ResponseEntity.badRequest().build();
    }

    Path path = queryBus.send(new GenerateOutletReportQuery(tenantId, id, d, d));
    if (path == null || !Files.exists(path)) {
      return ResponseEntity.notFound().build();
    }

    try {
      InputStreamResource resource = new InputStreamResource(Files.newInputStream(path));
      String filename = "outlet-report-" + d + ".csv";
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .contentLength(Files.size(path))
          .contentType(MediaType.parseMediaType("text/csv"))
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @Operation(summary = "Create outlet (admin)")
  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      @RequestHeader("X-Tenant-Id") TenantId tenantId, @RequestBody CreateOutletRequest req) {
    java.util.UUID id =
        commandBus.send(new CreateOutletCommand(tenantId, req.name(), req.slug(), req.address()));
    return ResponseEntity.ok(Map.of("id", id));
  }

  @Operation(summary = "Get outlet by id (admin)")
  @GetMapping("/{id}")
  public ResponseEntity<OutletDto> get(
      @PathVariable OutletId id, @RequestHeader("X-Tenant-Id") TenantId tenantId) {
    var dto = queryBus.send(new GetOutletByIdQuery(tenantId, id));
    return ResponseEntity.ok(dto);
  }
}

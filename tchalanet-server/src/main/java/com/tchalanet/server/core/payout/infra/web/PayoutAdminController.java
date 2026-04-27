package com.tchalanet.server.core.payout.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.application.command.model.ApprovePayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RejectPayoutCommand;
import com.tchalanet.server.core.payout.application.query.model.GeneratePayoutReportQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payout controller (workflow + execution).
 *
 * <p>Notes: - CRUD/listing payouts can be Data REST later if you want, but execution + workflow
 * stays here. - Wrappers are used in controller signatures thanks to Spring MVC converters. -
 * TenantId is required in requests (same pattern as your other admin controllers).
 */
@RestController
@RequestMapping("/admin/payouts")
@Tags({@Tag(name = "Admin • Payouts")})
public class PayoutAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public PayoutAdminController(CommandBus commandBus, QueryBus queryBus) {
    this.commandBus = commandBus;
    this.queryBus = queryBus;
  }

  // --------------------------------------------------------------------------
  // Execute payout (pay a winning ticket)
  // --------------------------------------------------------------------------
  // Suggested command signature (adjust if you already have it):
  // ExecutePayoutCommand(TenantId tenantId, TicketId ticketId, SessionId payingSessionId, UserId
  // paidBy)

  @Operation(summary = "Execute a payout (pay a winning ticket)")
  @PostMapping("/execute")
  public void execute(@RequestBody ExecutePayoutRequest body) {
    Objects.requireNonNull(body, "body");
    commandBus.send(new ExecutePayoutCommand(body.tenantId(), body.payoutId(), body.executedBy()));
  }

  public record ExecutePayoutRequest(
      TenantId tenantId,
      PayoutId payoutId,
      UserId executedBy // optional: can be null if derived from session
      ) {}

  // --------------------------------------------------------------------------
  // Register payout (create REQUESTED) - optional if you do create-on-execute
  // --------------------------------------------------------------------------
  @Operation(summary = "Register a payout request")
  @PostMapping("/request")
  public void request(@RequestBody RegisterPayoutRequest body) {
    Objects.requireNonNull(body, "body");
    commandBus.send(
        new RegisterPayoutCommand(
            body.tenantId(),
            body.ticketId(),
            body.payingOutletId(),
            body.payingSessionId(),
            body.terminalId(),
            body.paidBy(),
            body.reason()));
  }

  public record RegisterPayoutRequest(
      TenantId tenantId,
      TicketId ticketId,
      OutletId payingOutletId,
      SessionId payingSessionId,
      TerminalId terminalId,
      UserId paidBy,
      String reason,
      UserId requestedByUserId // optional (kept for backward compatibility)
      ) {}

  // --------------------------------------------------------------------------
  // Approve payout (REQUESTED -> APPROVED)
  // --------------------------------------------------------------------------
  @Operation(summary = "Approve a payout request")
  @PostMapping("/{payoutId}/approve")
  public void approve(@PathVariable PayoutId payoutId, @RequestBody ApprovePayoutRequest body) {
    Objects.requireNonNull(body, "body");
    commandBus.send(new ApprovePayoutCommand(body.tenantId(), payoutId));
  }

  public record ApprovePayoutRequest(TenantId tenantId, UserId approvedByUserId // optional
      ) {}

  // --------------------------------------------------------------------------
  // Reject payout (REQUESTED -> REJECTED)
  // --------------------------------------------------------------------------
  @Operation(summary = "Reject a payout request")
  @PostMapping("/{payoutId}/reject")
  public void reject(@PathVariable PayoutId payoutId, @RequestBody RejectPayoutRequest body) {
    Objects.requireNonNull(body, "body");
    commandBus.send(
        new RejectPayoutCommand(
            body.tenantId(), payoutId, body.reason(), body.rejectedByUserId(), body.rejectedAt()));
  }

  public record RejectPayoutRequest(
      TenantId tenantId,
      String reason,
      UserId rejectedByUserId, // optional
      Instant rejectedAt) {}

  // --------------------------------------------------------------------------
  // Report export
  // --------------------------------------------------------------------------
  // Returns a file download (CSV/PDF/XLSX depending on your report port).
  @Operation(summary = "Export payout report for a tenant (CSV/XLSX/PDF)")
  @GetMapping("/report")
  public ResponseEntity<Resource> report(
      @RequestParam TenantId tenantId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(required = false) com.tchalanet.server.common.types.id.OutletId outletId,
      @RequestParam(defaultValue = "CSV") String format) {
    Path path = queryBus.send(new GeneratePayoutReportQuery(tenantId, from, to, outletId, format));

    // naive content type mapping (V1)
    MediaType mt =
        switch (format == null ? "CSV" : format.toUpperCase()) {
          case "PDF" -> MediaType.APPLICATION_PDF;
          case "XLSX" ->
              MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
          default -> MediaType.parseMediaType("text/csv");
        };

    var resource = new FileSystemResource(path.toFile());
    return ResponseEntity.ok()
        .contentType(mt)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
        .body(resource);
  }
}

package com.tchalanet.server.platform.archive.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchivedTicketView;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only archive lookup endpoints for tenant/admin scope.
 *
 * <p>External route prefix: {@code /api/v1/admin/archive/**}. Controller mapping: {@code
 * /admin/archive/**} (no /api/v1).
 *
 * <p>All methods return {@code archived=true} in the view to signal to callers that the data comes
 * from the archive tier, not the hot database.
 */
@Tag(name = "Admin - Archive")
@RestController
@Validated
@RequestMapping("/admin/archive")
@PreAuthorize("hasPermission(null, 'archive.read')")
@RequiredArgsConstructor
public class AdminArchiveController {

  private final ArchiveApi archiveApi;

  @Operation(summary = "Find archived ticket by ID")
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.ARCHIVE_READ,
      idExpression = "#ticketId",
      detailsExpression = "#reason",
      tenantIdExpression = "#ctx.tenantIdRequired().value()")
  @GetMapping("/tickets/{ticketId}")
  public ApiResponse<ArchivedTicketView> getArchivedTicket(
      @PathVariable UUID ticketId,
      @RequestParam @NotBlank @Size(min = 10, max = 500) String reason,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedTicket(ctx.tenantIdRequired().value(), ticketId));
  }

  @Operation(summary = "Find archived ticket by public code")
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.ARCHIVE_READ,
      idExpression = "#publicCode",
      detailsExpression = "#reason",
      tenantIdExpression = "#ctx.tenantIdRequired().value()")
  @GetMapping("/tickets/by-public-code/{publicCode}")
  public ApiResponse<ArchivedTicketView> getArchivedTicketByCode(
      @PathVariable String publicCode,
      @RequestParam @NotBlank @Size(min = 10, max = 500) String reason,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedTicketByPublicCode(ctx.tenantIdRequired().value(), publicCode));
  }

  @Operation(summary = "Find archived payout by ID")
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.ARCHIVE_READ,
      idExpression = "#payoutId",
      detailsExpression = "#reason",
      tenantIdExpression = "#ctx.tenantIdRequired().value()")
  @GetMapping("/payouts/{payoutId}")
  public ApiResponse<ArchivedEntityView> getArchivedPayout(
      @PathVariable UUID payoutId,
      @RequestParam @NotBlank @Size(min = 10, max = 500) String reason,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedPayout(ctx.tenantIdRequired().value(), payoutId));
  }

  @Operation(summary = "Find archived audit records by entity")
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.ARCHIVE_READ,
      idExpression = "#entityId",
      detailsExpression = "#reason",
      tenantIdExpression = "#ctx.tenantIdRequired().value()")
  @GetMapping("/audit")
  public ApiResponse<List<ArchivedEntityView>> getArchivedAudit(
      @RequestParam String entityType,
      @RequestParam UUID entityId,
      @RequestParam @NotBlank @Size(min = 10, max = 500) String reason,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedAuditRecords(ctx.tenantIdRequired().value(), entityType, entityId));
  }
}

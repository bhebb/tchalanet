package com.tchalanet.server.platform.archive.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only archive lookup endpoints for tenant/admin scope.
 *
 * <p>External route prefix: {@code /api/v1/admin/archive/**}.
 * Controller mapping: {@code /admin/archive/**} (no /api/v1).
 *
 * <p>All methods return {@code archived=true} in the view to signal to callers
 * that the data comes from the archive tier, not the hot database.
 */
@Tag(name = "Admin - Archive")
@RestController
@RequestMapping("/admin/archive")
@PreAuthorize("hasPermission(null, 'archive.read')")
@RequiredArgsConstructor
public class AdminArchiveController {

  private final ArchiveApi archiveApi;

  @Operation(summary = "Find archived ticket by ID")
  @GetMapping("/tickets/{ticketId}")
  public ApiResponse<ArchivedEntityView> getArchivedTicket(
      @PathVariable UUID ticketId,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedTicket(ctx.effectiveTenantIdRequired().value(), ticketId));
  }

  @Operation(summary = "Find archived ticket by public code")
  @GetMapping("/tickets/by-public-code/{publicCode}")
  public ApiResponse<ArchivedEntityView> getArchivedTicketByCode(
      @PathVariable String publicCode,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedTicketByPublicCode(ctx.effectiveTenantIdRequired().value(), publicCode));
  }

  @Operation(summary = "Find archived payout by ID")
  @GetMapping("/payouts/{payoutId}")
  public ApiResponse<ArchivedEntityView> getArchivedPayout(
      @PathVariable UUID payoutId,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedPayout(ctx.effectiveTenantIdRequired().value(), payoutId));
  }

  @Operation(summary = "Find archived audit records by entity")
  @GetMapping("/audit")
  public ApiResponse<List<ArchivedEntityView>> getArchivedAudit(
      @RequestParam String entityType,
      @RequestParam UUID entityId,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.findArchivedAuditRecords(
            ctx.effectiveTenantIdRequired().value(), entityType, entityId));
  }
}

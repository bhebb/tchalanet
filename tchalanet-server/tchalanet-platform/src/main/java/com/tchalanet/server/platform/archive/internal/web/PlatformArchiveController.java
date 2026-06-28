package com.tchalanet.server.platform.archive.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.service.ArchiveDomainPurgeService;
import com.tchalanet.server.platform.archive.internal.service.ArchiveDomainPurgeService.DomainPurgeDataset;
import com.tchalanet.server.platform.archive.internal.service.ArchiveDomainPurgeService.DomainPurgeMode;
import com.tchalanet.server.platform.archive.internal.service.ArchiveDomainPurgeService.DomainPurgeResult;
import com.tchalanet.server.platform.archive.internal.service.ArchivePartitionCleanupService;
import com.tchalanet.server.platform.archive.internal.service.ArchivePartitionCleanupService.CleanupMode;
import com.tchalanet.server.platform.archive.internal.service.ArchivePartitionCleanupService.PartitionCleanupPlan;
import com.tchalanet.server.platform.archive.internal.service.ArchiveRestoreService;
import com.tchalanet.server.platform.archive.internal.service.ArchiveTicketPurgeService;
import com.tchalanet.server.platform.archive.internal.service.ArchiveTicketPurgeService.TicketPurgeMode;
import com.tchalanet.server.platform.archive.internal.service.ArchiveTicketPurgeService.TicketPurgeResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform archive management endpoints (SUPER_ADMIN only).
 *
 * <p>External route prefix: {@code /api/v1/platform/archive/**}.
 * Controller mapping: {@code /platform/archive/**} (no /api/v1).
 */
@Tag(name = "Platform - Archive")
@RestController
@RequestMapping("/platform/archive")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformArchiveController {

  private final ArchiveApi archiveApi;
  private final ArchiveRestoreService restoreService;
  private final ArchivePartitionCleanupService cleanupService;
  private final ArchiveTicketPurgeService ticketPurgeService;
  private final ArchiveDomainPurgeService domainPurgeService;
  private final ArchiveLegalHoldJdbcRepository legalHoldRepo;
  private final ArchiveRunJdbcRepository runRepo;
  private final ArchiveObjectJdbcRepository objectRepo;

  // ── Archive runs ────────────────────────────────────────────────────────────

  @Operation(summary = "Trigger an archive run")
  @PostMapping("/runs")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<ArchiveRunView> triggerRun(
      @Valid @RequestBody TriggerArchiveRunRequest request,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(
        archiveApi.triggerRun(request, ctx.currentUserIdRequired().value()));
  }

  @Operation(summary = "List recent archive runs")
  @GetMapping("/runs")
  public ApiResponse<List<ArchiveRunView>> listRuns(
      @RequestParam(defaultValue = "50") int limit) {

    return ApiResponse.success(archiveApi.listRuns(limit));
  }

  // ── Restore ─────────────────────────────────────────────────────────────────

  @Operation(summary = "Restore archived audit_log rows into temporary restore table")
  @PostMapping("/restore/audit-log")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ApiResponse<UUID> restoreAuditLog(
      @Valid @RequestBody RestoreAuditLogRequest request,
      @CurrentContext TchRequestContext ctx) {

    UUID restoreRunId = restoreService.restoreAuditLog(
        request.tenantId(),
        request.entityType(),
        request.entityId(),
        request.from(),
        request.to(),
        ctx.currentUserIdRequired().value(),
        request.reason());

    return ApiResponse.success(restoreRunId);
  }

  // ── Partition cleanup ───────────────────────────────────────────────────────

  @Operation(summary = "Plan partition cleanup (dry-run safe)")
  @GetMapping("/partition-cleanup/plan")
  public ApiResponse<List<PartitionCleanupPlan>> partitionCleanupPlan(
      @RequestParam(defaultValue = "audit_log") String tableName,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate retentionCutoff) {

    return ApiResponse.success(cleanupService.plan(tableName, retentionCutoff));
  }

  @Operation(summary = "Execute cleanup for a specific partition (DRY_RUN by default)")
  @PostMapping("/partition-cleanup/execute")
  public ApiResponse<Void> executePartitionCleanup(
      @RequestParam @NotBlank String partitionName,
      @RequestParam(defaultValue = "DRY_RUN") CleanupMode mode) {

    cleanupService.executeCleanup(partitionName, mode);
    return ApiResponse.success(null);
  }

  @Operation(summary = "Purge archived ticket rows from hot storage (DRY_RUN by default)")
  @PostMapping("/ticket-purge")
  public ApiResponse<TicketPurgeResult> purgeArchivedTickets(
      @Valid @RequestBody TicketPurgeRequest request,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(ticketPurgeService.purge(
        request.tenantId(),
        request.periodStart(),
        request.periodEnd(),
        request.batchSize() == null ? 5_000 : request.batchSize(),
        request.mode() == null ? TicketPurgeMode.DRY_RUN : request.mode(),
        ctx.currentUserIdRequired().value(),
        request.reason()));
  }

  @Operation(summary = "Purge archived draw, draw_result or Envers revision rows (DRY_RUN by default)")
  @PostMapping("/domain-purge")
  public ApiResponse<DomainPurgeResult> purgeArchivedDomainRows(
      @Valid @RequestBody DomainPurgeRequest request,
      @CurrentContext TchRequestContext ctx) {

    return ApiResponse.success(domainPurgeService.purge(
        request.dataset(),
        request.tenantId(),
        request.periodStart(),
        request.periodEnd(),
        request.batchSize() == null ? 5_000 : request.batchSize(),
        request.mode() == null ? DomainPurgeMode.DRY_RUN : request.mode(),
        ctx.currentUserIdRequired().value(),
        request.reason()));
  }

  // ── Legal holds ────────────────────────────────────────────────────────────

  @Operation(summary = "Create archive legal hold")
  @PostMapping("/legal-holds")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<UUID> createLegalHold(
      @Valid @RequestBody CreateLegalHoldRequest request,
      @CurrentContext TchRequestContext ctx) {

    UUID id = legalHoldRepo.create(
        request.tenantId(),
        request.datasetCode(),
        request.entityType(),
        request.entityId(),
        request.periodStart(),
        request.periodEnd(),
        request.reason(),
        ctx.currentUserIdRequired().value());
    return ApiResponse.success(id);
  }

  @Operation(summary = "Release archive legal hold")
  @PostMapping("/legal-holds/{holdId}/release")
  public ApiResponse<Void> releaseLegalHold(
      @org.springframework.web.bind.annotation.PathVariable UUID holdId,
      @Valid @RequestBody ReleaseLegalHoldRequest request,
      @CurrentContext TchRequestContext ctx) {

    legalHoldRepo.release(holdId, ctx.currentUserIdRequired().value(), request.reason());
    return ApiResponse.success(null);
  }

  @Operation(summary = "List active archive legal holds")
  @GetMapping("/legal-holds/active")
  public ApiResponse<List<Map<String, Object>>> listActiveLegalHolds(
      @RequestParam(defaultValue = "50") int limit) {
    return ApiResponse.success(legalHoldRepo.listActive(limit));
  }

  // ── Ops view ────────────────────────────────────────────────────────────────

  @Operation(summary = "List failed archive runs")
  @GetMapping("/runs/failed")
  public ApiResponse<List<Map<String, Object>>> listFailedRuns(
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.success(runRepo.listFailed(limit));
  }

  @Operation(summary = "List invalid archive objects")
  @GetMapping("/objects/invalid")
  public ApiResponse<List<Map<String, Object>>> listInvalidObjects(
      @RequestParam(defaultValue = "20") int limit) {
    return ApiResponse.success(objectRepo.listInvalid(limit));
  }

  @Operation(summary = "Archive system ops summary")
  @GetMapping("/ops-summary")
  public ApiResponse<Map<String, Object>> opsSummary() {
    return ApiResponse.success(Map.of(
        "failedRuns",     runRepo.countByStatus("FAILED"),
        "startedRuns",    runRepo.countByStatus("STARTED"),
        "completedRuns",  runRepo.countByStatus("COMPLETED"),
        "invalidObjects", objectRepo.countByStatus("INVALID"),
        "verifiedObjects", objectRepo.countByStatus("VERIFIED"),
        "pendingObjects", objectRepo.countByStatus("PENDING")
    ));
  }

  // ── Request records ─────────────────────────────────────────────────────────

  public record RestoreAuditLogRequest(
      UUID tenantId,
      @NotBlank String entityType,
      @NotNull UUID entityId,
      @NotNull LocalDate from,
      @NotNull LocalDate to,
      @NotBlank @Size(min = 10, max = 500) String reason
  ) {}

  public record CreateLegalHoldRequest(
      UUID tenantId,
      @NotBlank String datasetCode,
      String entityType,
      String entityId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
      @NotBlank @Size(min = 10, max = 1000) String reason
  ) {}

  public record ReleaseLegalHoldRequest(
      @NotBlank @Size(min = 10, max = 1000) String reason
  ) {}

  public record TicketPurgeRequest(
      UUID tenantId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
      Integer batchSize,
      TicketPurgeMode mode,
      @NotBlank @Size(min = 10, max = 1000) String reason
  ) {}

  public record DomainPurgeRequest(
      @NotNull DomainPurgeDataset dataset,
      UUID tenantId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
      Integer batchSize,
      DomainPurgeMode mode,
      @NotBlank @Size(min = 10, max = 1000) String reason
  ) {}
}

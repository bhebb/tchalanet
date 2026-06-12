package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.ArchiveDatasetProvider;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Archive orchestration service — entry point for all archive operations.
 *
 * <p>Providers are injected as {@code List<ArchiveDatasetProvider>}; this service
 * never imports provider implementation classes directly (enforced by ArchUnit gate).
 *
 * <p>Object-storage integration, run persistence and restore logic are TODO (Phase 5b/6b).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveService implements ArchiveApi {

  // All ArchiveDatasetProvider beans discovered via Spring injection
  @SuppressWarnings("unused")
  private final List<ArchiveDatasetProvider> providers;

  // ── Lookup ─────────────────────────────────────────────────────────────────

  @Override
  public ArchivedEntityView findArchivedTicket(UUID tenantId, UUID ticketId) {
    log.debug("archive: findArchivedTicket tenant={} ticket={}", tenantId, ticketId);
    // TODO: query archive_lookup_index WHERE entity_type='TICKET' AND entity_id=ticketId
    //       AND tenant_id=tenantId, then fetch from object storage via SalesTicketArchiveDatasetProvider
    return ArchivedEntityView.notFound(ticketId);
  }

  @Override
  public ArchivedEntityView findArchivedTicketByPublicCode(UUID tenantId, String publicCode) {
    log.debug("archive: findArchivedTicketByPublicCode tenant={} code={}", tenantId, publicCode);
    // TODO: query archive_lookup_index WHERE public_code=publicCode AND tenant_id=tenantId
    return ArchivedEntityView.notFound(null);
  }

  @Override
  public ArchivedEntityView findArchivedPayout(UUID tenantId, UUID payoutId) {
    log.debug("archive: findArchivedPayout tenant={} payout={}", tenantId, payoutId);
    // TODO: query archive_lookup_index WHERE entity_type='PAYOUT' AND entity_id=payoutId
    return ArchivedEntityView.notFound(payoutId);
  }

  @Override
  public List<ArchivedEntityView> findArchivedAuditRecords(
      UUID tenantId, String entityType, UUID entityId) {
    log.debug("archive: findArchivedAuditRecords tenant={} entity={}:{}", tenantId, entityType, entityId);
    // TODO: query archive_lookup_index WHERE table_name='audit_log' AND entity_type=entityType
    //       AND entity_id=entityId AND tenant_id=tenantId
    return List.of();
  }

  // ── Run management ─────────────────────────────────────────────────────────

  @Override
  public ArchiveRunView triggerRun(TriggerArchiveRunRequest request, UUID requestedBy) {
    log.info("archive: triggerRun strategy={} period={}/{} by={}",
        request.strategy(), request.periodStart(), request.periodEnd(), requestedBy);
    // TODO: insert archive_run row, validate idempotency_key, dispatch to providers
    throw new UnsupportedOperationException(
        "archive run execution not yet implemented — requires object-storage integration");
  }

  @Override
  public List<ArchiveRunView> listRuns(int limit) {
    // TODO: query archive_run table with pagination
    return List.of();
  }
}

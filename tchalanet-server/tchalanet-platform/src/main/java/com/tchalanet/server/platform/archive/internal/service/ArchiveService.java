package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Archive service — façade over {@link ArchiveRunExecutor} and lookup repositories.
 *
 * <p>Providers are discovered via {@link ArchiveRunExecutor}'s injected
 * {@code List<ArchiveDatasetProvider>}; this service never imports provider
 * implementation classes (ArchUnit-enforced).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveService implements ArchiveApi {

  private final ArchiveRunExecutor executor;
  private final ArchiveRunJdbcRepository runRepo;

  // ── Lookup ─────────────────────────────────────────────────────────────────

  @Override
  public ArchivedEntityView findArchivedTicket(UUID tenantId, UUID ticketId) {
    log.debug("archive: findArchivedTicket tenant={} ticket={}", tenantId, ticketId);
    // TODO Phase 7C: query archive_lookup_index WHERE entity_type='TICKET' AND entity_id=ticketId
    return ArchivedEntityView.notFound(ticketId);
  }

  @Override
  public ArchivedEntityView findArchivedTicketByPublicCode(UUID tenantId, String publicCode) {
    log.debug("archive: findArchivedTicketByPublicCode tenant={} code={}", tenantId, publicCode);
    // TODO Phase 7C: query archive_lookup_index WHERE public_code=publicCode
    return ArchivedEntityView.notFound(null);
  }

  @Override
  public ArchivedEntityView findArchivedPayout(UUID tenantId, UUID payoutId) {
    log.debug("archive: findArchivedPayout tenant={} payout={}", tenantId, payoutId);
    // TODO Phase 7C: query archive_lookup_index WHERE entity_type='PAYOUT' AND entity_id=payoutId
    return ArchivedEntityView.notFound(payoutId);
  }

  @Override
  public List<ArchivedEntityView> findArchivedAuditRecords(
      UUID tenantId, String entityType, UUID entityId) {
    log.debug("archive: findArchivedAuditRecords tenant={} entity={}:{}", tenantId, entityType, entityId);
    // TODO Phase 7C: query archive_lookup_index + fetch and scan archive objects
    return List.of();
  }

  // ── Run management ─────────────────────────────────────────────────────────

  @Override
  public ArchiveRunView triggerRun(TriggerArchiveRunRequest request, UUID requestedBy) {
    log.info("archive: triggerRun strategy={} period={}/{} by={}",
        request.strategy(), request.periodStart(), request.periodEnd(), requestedBy);
    return executor.execute(request, requestedBy);
  }

  @Override
  public List<ArchiveRunView> listRuns(int limit) {
    return runRepo.listRecent(limit).stream()
        .map(this::toRunView)
        .toList();
  }

  private ArchiveRunView toRunView(Map<String, Object> row) {
    return new ArchiveRunView(
        (UUID) row.get("id"),
        (String) row.get("status"),
        (String) row.get("strategy"),
        (String) row.get("trigger_type"),
        (String) row.get("idempotency_key"),
        toInstant(row.get("started_at")),
        toInstant(row.get("completed_at")),
        (String) row.get("error_message")
    );
  }

  private static java.time.Instant toInstant(Object val) {
    if (val == null) return null;
    if (val instanceof Timestamp ts) return ts.toInstant();
    if (val instanceof java.time.Instant i) return i;
    return null;
  }
}

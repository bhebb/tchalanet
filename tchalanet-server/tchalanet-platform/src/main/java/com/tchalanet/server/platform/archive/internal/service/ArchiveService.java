package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.io.JsonlGzReader;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

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
  private final ArchiveLookupIndexJdbcRepository lookupRepo;
  private final ArchiveObjectJdbcRepository objectRepo;
  private final ArchiveStoragePort storage;
  private final ObjectMapper objectMapper;

  // ── Lookup ─────────────────────────────────────────────────────────────────

  @Override
  public ArchivedEntityView findArchivedTicket(UUID tenantId, UUID ticketId) {
    log.debug("archive: findArchivedTicket tenant={} ticket={}", tenantId, ticketId);
    // TODO Phase 8: ticket archive integration
    return ArchivedEntityView.notFound(ticketId);
  }

  @Override
  public ArchivedEntityView findArchivedTicketByPublicCode(UUID tenantId, String publicCode) {
    log.debug("archive: findArchivedTicketByPublicCode tenant={} code={}", tenantId, publicCode);
    // TODO Phase 8: ticket archive integration
    return ArchivedEntityView.notFound(null);
  }

  @Override
  public ArchivedEntityView findArchivedPayout(UUID tenantId, UUID payoutId) {
    log.debug("archive: findArchivedPayout tenant={} payout={}", tenantId, payoutId);
    // TODO Phase 9: payout archive integration
    return ArchivedEntityView.notFound(payoutId);
  }

  @Override
  public List<ArchivedEntityView> findArchivedAuditRecords(
      UUID tenantId, String entityType, UUID entityId) {
    log.debug("archive: findArchivedAuditRecords tenant={} entity={}:{}", tenantId, entityType, entityId);

    List<Map<String, Object>> lookupEntries =
        lookupRepo.findByEntity("audit_log", entityType, entityId);

    if (lookupEntries.isEmpty()) {
      return List.of();
    }

    String entityIdStr = entityId.toString();
    JsonlGzReader reader = new JsonlGzReader(objectMapper);
    List<ArchivedEntityView> results = new ArrayList<>();

    for (Map<String, Object> entry : lookupEntries) {
      UUID objectId = (UUID) entry.get("archive_object_id");
      Optional<Map<String, Object>> objMeta = objectRepo.findById(objectId);
      if (objMeta.isEmpty()) {
        log.warn("archive: lookup entry points to missing archive_object id={}", objectId);
        continue;
      }

      Map<String, Object> obj = objMeta.get();
      String uri = (String) obj.get("object_uri");
      int schemaVersion = ((Number) obj.getOrDefault("schema_version", 1)).intValue();

      if (!storage.exists(uri)) {
        log.warn("archive: archive object not found in storage uri={}", uri);
        continue;
      }

      try (InputStream in = storage.openRead(uri)) {
        List<Map<String, Object>> rows = reader.readMatching(in, row ->
            entityType.equals(row.get("entity_type"))
                && entityIdStr.equals(String.valueOf(row.get("entity_id"))));

        if (!rows.isEmpty()) {
          results.add(new ArchivedEntityView(
              true, entityId, null, "audit_log", schemaVersion, uri, rows));
        }
      } catch (Exception ex) {
        log.error("archive: failed to read archive object uri={}: {}", uri, ex.getMessage(), ex);
      }
    }

    log.info("archive: findArchivedAuditRecords entity={}:{} → {} objects scanned, {} with hits",
        entityType, entityId, lookupEntries.size(), results.size());
    return results;
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

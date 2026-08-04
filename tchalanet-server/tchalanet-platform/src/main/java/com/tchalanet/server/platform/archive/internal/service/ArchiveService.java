package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.archive.api.ArchiveApi;
import com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunRowView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchivedTicketView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import com.tchalanet.server.platform.archive.internal.io.JsonlGzReader;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLookupIndexJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import com.tchalanet.server.platform.archive.internal.storage.ArchiveStoragePort;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Archive service — façade over {@link ArchiveRunExecutor} and lookup repositories.
 *
 * <p>Providers are discovered via {@link ArchiveRunExecutor}'s injected {@code
 * List<ArchiveDatasetProvider>}; this service never imports provider implementation classes
 * (ArchUnit-enforced).
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
  private final JsonUtils jsonUtils;
  private final ArchiveMetrics metrics;

  // ── Lookup ─────────────────────────────────────────────────────────────────

  @Override
  public ArchivedTicketView findArchivedTicket(UUID tenantId, UUID ticketId) {
    log.debug("archive: findArchivedTicket tenant={} ticket={}", tenantId, ticketId);
    metrics.recordLookupFallback();
    return lookupTicketByEntity(tenantId, ticketId, null);
  }

  @Override
  public ArchivedTicketView findArchivedTicketByPublicCode(UUID tenantId, String publicCode) {
    log.debug("archive: findArchivedTicketByPublicCode tenant={} code={}", tenantId, publicCode);
    metrics.recordLookupFallback();
    return lookupTicketByPublicCode(tenantId, publicCode);
  }

  private ArchivedTicketView lookupTicketByEntity(UUID tenantId, UUID ticketId, String publicCode) {
    List<Map<String, Object>> entries = lookupRepo.findByEntity("sales_ticket", "TICKET", ticketId);
    return readFirstMatchingTicket(tenantId, ticketId, publicCode, entries);
  }

  private ArchivedTicketView lookupTicketByPublicCode(UUID tenantId, String publicCode) {
    List<Map<String, Object>> entries = lookupRepo.findByPublicCode("sales_ticket", publicCode);
    return readFirstMatchingTicket(tenantId, null, publicCode, entries);
  }

  private ArchivedTicketView readFirstMatchingTicket(
      UUID tenantId, UUID ticketId, String publicCode, List<Map<String, Object>> entries) {

    if (entries.isEmpty()) return ArchivedTicketView.notFound(ticketId);

    String tenantStr = tenantId != null ? tenantId.toString() : null;
    String ticketIdStr = ticketId != null ? ticketId.toString() : null;
    JsonlGzReader reader = new JsonlGzReader(jsonUtils);

    for (Map<String, Object> entry : entries) {
      UUID objectId = (UUID) entry.get("archive_object_id");
      Optional<ArchiveObjectRowView> objMeta = objectRepo.findById(objectId);
      if (objMeta.isEmpty()) continue;

      ArchiveObjectRowView object = objMeta.get();
      if (!"sales_ticket".equals(object.tableName()) || !"VERIFIED".equals(object.status())) {
        log.warn(
            "archive: refusing ticket lookup through non-verified object id={} table={} status={}",
            objectId,
            object.tableName(),
            object.status());
        continue;
      }

      String uri = object.objectUri();
      int schemaVersion = object.schemaVersion();
      if (!storage.exists(uri)) continue;

      try (InputStream in = storage.openRead(uri)) {
        List<Map<String, Object>> rows =
            reader.readMatching(
                in,
                row -> {
                  if (tenantStr != null && !tenantStr.equals(String.valueOf(row.get("tenant_id"))))
                    return false;
                  if (ticketIdStr != null && !ticketIdStr.equals(String.valueOf(row.get("id"))))
                    return false;
                  if (publicCode != null && !publicCode.equals(row.get("public_code")))
                    return false;
                  return true;
                });

        if (!rows.isEmpty()) {
          Map<String, Object> header = rows.get(0);
          UUID foundId = toUuid(header.get("id"));
          return toArchivedTicket(header, foundId, object);
        }
      } catch (Exception ex) {
        log.error("archive: failed reading ticket object uri={}: {}", uri, ex.getMessage(), ex);
        metrics.recordObjectReadError("sales_ticket");
      }
    }
    return ArchivedTicketView.notFound(ticketId);
  }

  private static ArchivedTicketView toArchivedTicket(
      Map<String, Object> header, UUID ticketId, ArchiveObjectRowView object) {
    return new ArchivedTicketView(
        true,
        ticketId,
        stringValue(header.get("public_code")),
        toUuid(header.get("tenant_id")),
        instantValue(header.get("sold_at")),
        stringValue(header.get("sale_status")),
        stringValue(header.get("result_status")),
        stringValue(header.get("settlement_status")),
        stringValue(header.get("currency")),
        header,
        List.of(),
        List.of(),
        new ArchivedTicketView.ArchiveObjectMeta(
            object.id(), object.periodStart(), object.periodEnd(), object.schemaVersion()));
  }

  private static String stringValue(Object value) {
    return value == null ? null : value.toString();
  }

  private static Instant instantValue(Object value) {
    if (value == null) return null;
    if (value instanceof Instant instant) return instant;
    if (value instanceof java.util.Date date) return date.toInstant();
    try {
      return Instant.parse(value.toString());
    } catch (java.time.format.DateTimeParseException ex) {
      return null;
    }
  }

  private static UUID toUuid(Object val) {
    if (val == null) return null;
    if (val instanceof UUID u) return u;
    try {
      return UUID.fromString(val.toString());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public ArchivedEntityView findArchivedPayout(UUID tenantId, UUID payoutId) {
    log.debug("archive: findArchivedPayout tenant={} payout={}", tenantId, payoutId);
    metrics.recordLookupFallback();
    List<Map<String, Object>> entries = lookupRepo.findByEntity("payout", "PAYOUT", payoutId);
    if (entries.isEmpty()) return ArchivedEntityView.notFound(payoutId);

    String tenantStr = tenantId != null ? tenantId.toString() : null;
    String payoutStr = payoutId.toString();
    JsonlGzReader reader = new JsonlGzReader(jsonUtils);

    for (Map<String, Object> entry : entries) {
      UUID objectId = (UUID) entry.get("archive_object_id");
      Optional<ArchiveObjectRowView> objMeta = objectRepo.findById(objectId);
      if (objMeta.isEmpty()) continue;

      String uri = objMeta.get().objectUri();
      int schemaVersion = objMeta.get().schemaVersion();
      if (!storage.exists(uri)) continue;

      try (InputStream in = storage.openRead(uri)) {
        List<Map<String, Object>> rows =
            reader.readMatching(
                in,
                row -> {
                  if (tenantStr != null && !tenantStr.equals(String.valueOf(row.get("tenant_id"))))
                    return false;
                  return payoutStr.equals(String.valueOf(row.get("id")));
                });

        if (!rows.isEmpty()) {
          UUID foundId = toUuid(rows.get(0).get("id"));
          return new ArchivedEntityView(true, foundId, null, "payout", schemaVersion, null, rows);
        }
      } catch (Exception ex) {
        log.error("archive: failed reading payout object uri={}: {}", uri, ex.getMessage(), ex);
        metrics.recordObjectReadError("payout");
      }
    }
    return ArchivedEntityView.notFound(payoutId);
  }

  @Override
  public List<ArchivedEntityView> findArchivedAuditRecords(
      UUID tenantId, String entityType, UUID entityId) {
    log.debug(
        "archive: findArchivedAuditRecords tenant={} entity={}:{}", tenantId, entityType, entityId);

    List<Map<String, Object>> lookupEntries =
        lookupRepo.findByEntity("audit_log", entityType, entityId);

    if (lookupEntries.isEmpty()) {
      return List.of();
    }

    String entityIdStr = entityId.toString();
    JsonlGzReader reader = new JsonlGzReader(jsonUtils);
    List<ArchivedEntityView> results = new ArrayList<>();

    for (Map<String, Object> entry : lookupEntries) {
      UUID objectId = (UUID) entry.get("archive_object_id");
      Optional<ArchiveObjectRowView> objMeta = objectRepo.findById(objectId);
      if (objMeta.isEmpty()) {
        log.warn("archive: lookup entry points to missing archive_object id={}", objectId);
        continue;
      }

      ArchiveObjectRowView obj = objMeta.get();
      String uri = obj.objectUri();
      int schemaVersion = obj.schemaVersion();

      if (!storage.exists(uri)) {
        log.warn("archive: archive object not found in storage uri={}", uri);
        continue;
      }

      try (InputStream in = storage.openRead(uri)) {
        List<Map<String, Object>> rows =
            reader.readMatching(
                in,
                row ->
                    entityType.equals(row.get("entity_type"))
                        && entityIdStr.equals(String.valueOf(row.get("entity_id"))));

        if (!rows.isEmpty()) {
          results.add(
              new ArchivedEntityView(true, entityId, null, "audit_log", schemaVersion, uri, rows));
        }
      } catch (Exception ex) {
        log.error("archive: failed to read archive object uri={}: {}", uri, ex.getMessage(), ex);
        metrics.recordObjectReadError("audit_log");
      }
    }

    log.info(
        "archive: findArchivedAuditRecords entity={}:{} → {} objects scanned, {} with hits",
        entityType,
        entityId,
        lookupEntries.size(),
        results.size());
    return results;
  }

  // ── Run management ─────────────────────────────────────────────────────────

  @Override
  public ArchiveRunView triggerRun(TriggerArchiveRunRequest request, UUID requestedBy) {
    log.info(
        "archive: triggerRun strategy={} period={}/{} by={}",
        request.strategy(),
        request.periodStart(),
        request.periodEnd(),
        requestedBy);
    return executor.execute(request, requestedBy);
  }

  @Override
  public List<ArchiveRunView> listRuns(int limit) {
    return runRepo.listRecent(limit).stream().map(this::toRunView).toList();
  }

  private ArchiveRunView toRunView(ArchiveRunRowView row) {
    return new ArchiveRunView(
        row.id(),
        row.status(),
        row.strategy(),
        row.triggerType(),
        row.idempotencyKey(),
        row.startedAt(),
        row.completedAt(),
        row.errorMessage());
  }
}

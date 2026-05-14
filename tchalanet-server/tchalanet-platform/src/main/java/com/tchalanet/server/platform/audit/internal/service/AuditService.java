package com.tchalanet.server.platform.audit.internal.service;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.platform.audit.api.model.ActivityItemDto;
import com.tchalanet.server.platform.audit.api.model.AuditEventView;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import com.tchalanet.server.platform.audit.api.model.request.AuditEventRequest;
import com.tchalanet.server.platform.audit.api.model.request.ListAuditEventsRequest;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Application service for the audit capability. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

  private final AuditEventReaderPort reader;
  private final AuditEventWriterPort writer;
  private final AuditEventFactory factory;
  private final Clock clock;

  @Value("${tch.audit.retention-days:90}")
  private int retentionDays;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logAuditEvent(LogAuditEventRequest request) {
    if (request == null) {
      return;
    }
    var event =
        factory.build(
            request.entityType(), request.entityId(), request.action(), request.details());
    if (event == null) {
      return;
    }
    try {
      writer.save(event);
    } catch (Exception e) {
      log.error("Failed to write audit event", e);
    }
  }

  @Transactional(readOnly = true)
  public TchPage<AuditEventView> listAuditEvents(ListAuditEventsRequest request) {
    var page =
        reader.findByCriteria(
            new AuditEventsCriteria(
                request.tenantId(),
                request.entityType(),
                normalize(request.entityId()),
                request.action(),
                normalize(request.actorId()),
                request.from(),
                request.to(),
                request.pageable()));
    return TchPageMapper.map(page, AuditEventMapper::toView);
  }

  @Transactional(readOnly = true)
  public List<AuditEventView> listRecentAuditEvents(AuditEventRequest request) {
    return reader.findRecentForTenant(request.tenant(), request.limit()).stream()
        .map(AuditEventMapper::toView)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ActivityItemDto> listTenantRecentActivity(AuditEventRequest request) {
    return reader.findRecentForTenant(request.tenant(), request.limit()).stream()
        .map(
            ev ->
                new ActivityItemDto(
                    ev.id(),
                    ev.occurredAt(),
                    ev.entityType(),
                    ev.entityId(),
                    ev.action(),
                    ev.actorType(),
                    ev.actorId().toString(),
                    ev.action().name() + " " + ev.entityType().name() + "/" + ev.entityId(),
                    ev.detailsJson()))
        .collect(Collectors.toList());
  }

  @TchTx
  public PurgeOldAuditEventsResult purgeOldAuditEvents(PurgeOldAuditEventsRequest request) {
    var threshold = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
    int deleted = writer.deleteBefore(threshold);
    log.info(
        "Purged {} audit events older than {} days (threshold={})",
        deleted,
        retentionDays,
        threshold);
    return new PurgeOldAuditEventsResult(deleted, retentionDays, threshold);
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

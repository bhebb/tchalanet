package com.tchalanet.server.audit.infra.persistence.adapter;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.ports.out.AuditEventReaderPort;
import com.tchalanet.server.audit.domain.ports.out.AuditEventWriterPort;
import com.tchalanet.server.audit.infra.persistence.AuditEventJpaEntity;
import com.tchalanet.server.audit.infra.persistence.AuditEventSpringRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventRepositoryAdapter implements AuditEventReaderPort, AuditEventWriterPort {

  private final AuditEventSpringRepository jpa;

  @Override
  public AuditEvent save(AuditEvent event) {
    AuditEventJpaEntity entity = toEntity(event);
    AuditEventJpaEntity saved = jpa.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<AuditEvent> findRecentForTenant(UUID tenantId, int limit) {
    return jpa.findTop100ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId).stream()
        .limit(limit)
        .map(this::toDomain)
        .toList();
  }

  @Override
  public int deleteBefore(Instant threshold) {
    return jpa.deleteByOccurredAtBefore(threshold);
  }

  private AuditEventJpaEntity toEntity(AuditEvent event) {
    AuditEventJpaEntity e = new AuditEventJpaEntity();
    // id may be null for new entities; keep tenantId + actor info
    e.setId(event.id()); // may be null
    e.setTenantId(event.tenantId());
    e.setActorType(event.actorType().name());
    e.setActorId(event.actorId());
    e.setEntityType(event.entityType().name());
    e.setEntityId(event.entityId());
    e.setAction(event.action().name());
    e.setDetails(event.detailsJson());
    e.setIp(event.ip());
    e.setUserAgent(event.userAgent());
    return e;
  }

  private AuditEvent toDomain(AuditEventJpaEntity e) {
    return new AuditEvent(
        e.getId(),
        e.getTenantId(),
        e.getCreatedAt(),
        e.getCreatedBy(),
        AuditActorType.valueOf(e.getActorType()),
        e.getActorId(),
        AuditEntityType.valueOf(e.getEntityType()),
        e.getEntityId(),
        AuditAction.valueOf(e.getAction()),
        e.getDetails(),
        e.getIp(),
        e.getUserAgent());
  }
}

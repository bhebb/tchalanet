package com.tchalanet.server.common.audit.infra.persistence;

// common.audit.infra.persistence.AuditEventRepositoryAdapter

import com.tchalanet.server.common.audit.domain.model.AuditAction;
import com.tchalanet.server.common.audit.domain.model.AuditActorType;
import com.tchalanet.server.common.audit.domain.model.AuditEntityType;
import com.tchalanet.server.common.audit.domain.model.AuditEvent;
import com.tchalanet.server.common.audit.domain.ports.AuditEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventRepositoryAdapter implements AuditEventRepository {

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
    // Do not set createdAt/createdBy here: BaseEntity @PrePersist will set createdAt and createdBy
    // is managed by auditor
    // e.setCreatedAt(event.createdAt());
    // e.setCreatedBy(event.createdBy());
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

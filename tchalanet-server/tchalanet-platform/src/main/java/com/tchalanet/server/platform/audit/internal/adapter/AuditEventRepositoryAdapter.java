package com.tchalanet.server.platform.audit.internal.adapter;

import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditActorType;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.platform.audit.internal.persistence.AuditEventJpaEntity;
import com.tchalanet.server.platform.audit.internal.persistence.AuditEventJpaRepository;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import com.tchalanet.server.platform.audit.internal.service.AuditEventReaderPort;
import com.tchalanet.server.platform.audit.internal.service.AuditEventWriterPort;
import com.tchalanet.server.platform.audit.internal.service.AuditEventsCriteria;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventRepositoryAdapter implements AuditEventReaderPort, AuditEventWriterPort {

  private final AuditEventJpaRepository jpa;

  @Override
  public AuditEvent save(AuditEvent event) {
    AuditEventJpaEntity entity = toEntity(event);
    AuditEventJpaEntity saved = jpa.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<AuditEvent> findRecentForTenant(TenantId tenantId, int limit) {
    return jpa.findTop100ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId.uuid()).stream()
        .limit(limit)
        .map(this::toDomain)
        .toList();
  }

  @Override
  public TchPage<AuditEvent> findByCriteria(AuditEventsCriteria criteria) {
    var pageable = criteria == null || criteria.pageable() == null
        ? PageRequest.of(0, 20)
        : criteria.pageable();

    var page = jpa.findAll(toSpecification(criteria), pageable);
    return TchPageMapper.map(page, this::toDomain);
  }

  @Override
  public int deleteBefore(Instant threshold) {
    return jpa.deleteByOccurredAtBefore(threshold);
  }

  private AuditEventJpaEntity toEntity(AuditEvent event) {
    AuditEventJpaEntity e = new AuditEventJpaEntity();
    // id may be null for new entities; keep tenantId + actor info
    e.setId(event.id()); // may be null
    e.setTenantId(event.tenantId() == null ? null : event.tenantId().uuid());
    e.setOccurredAt(event.occurredAt());
    e.setActorType(event.actorType().name());
    e.setActorId(event.actorId() == null ? null : event.actorId().toString());
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
        com.tchalanet.server.common.types.id.TenantId.nullableOf(e.getTenantId()),
        e.getOccurredAt(),
        e.getCreatedBy(),
        AuditActorType.valueOf(e.getActorType()),
        e.getActorId() == null ? null : UUID.fromString(e.getActorId()),
        AuditEntityType.valueOf(e.getEntityType()),
        e.getEntityId(),
        AuditAction.valueOf(e.getAction()),
        e.getDetails(),
        e.getIp(),
        e.getUserAgent());
  }

  private static Specification<AuditEventJpaEntity> toSpecification(AuditEventsCriteria criteria) {
    return (root, query, cb) -> {
      var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      predicates.add(cb.isNull(root.get("deletedAt")));

      if (criteria != null) {
        if (criteria.tenantId() != null) {
          predicates.add(cb.equal(root.get("tenantId"), criteria.tenantId().value()));
        }
        if (criteria.entityType() != null) {
          predicates.add(cb.equal(root.get("entityType"), criteria.entityType().name()));
        }
        if (criteria.entityId() != null && !criteria.entityId().isBlank()) {
          predicates.add(cb.equal(root.get("entityId"), criteria.entityId().trim()));
        }
        if (criteria.action() != null) {
          predicates.add(cb.equal(root.get("action"), criteria.action().name()));
        }
        if (criteria.actorId() != null && !criteria.actorId().isBlank()) {
          predicates.add(cb.equal(root.get("actorId"), criteria.actorId().trim()));
        }
        if (criteria.from() != null) {
          predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
        }
        if (criteria.to() != null) {
          predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.to()));
        }
      }

      return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }
}

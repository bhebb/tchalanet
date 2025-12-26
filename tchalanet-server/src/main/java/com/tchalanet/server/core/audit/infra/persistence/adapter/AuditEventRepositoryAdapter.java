package com.tchalanet.server.core.audit.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.core.audit.application.port.out.AuditEventReaderPort;
import com.tchalanet.server.core.audit.application.port.out.AuditEventWriterPort;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditActorType;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.core.audit.domain.model.AuditEvent;
import com.tchalanet.server.core.audit.infra.persistence.AuditEventJpaEntity;
import com.tchalanet.server.core.audit.infra.persistence.AuditEventSpringRepository;

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
    public List<AuditEvent> findRecentForTenant(TenantId tenantId, int limit) {
        return jpa.findTop100ByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId.uuid()).stream()
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
        e.setTenantId(event.tenantId().uuid());
        e.setActorType(event.actorType().name());
        e.setActorId(event.actorId().toString());
        e.setEntityType(event.entityType().name());
        e.setEntityId(event.entityId().toString());
        e.setAction(event.action().name());
        e.setDetails(event.detailsJson());
        e.setIp(event.ip());
        e.setUserAgent(event.userAgent());
        return e;
    }

    private AuditEvent toDomain(AuditEventJpaEntity e) {
        return new AuditEvent(
            e.getId(),
            com.tchalanet.server.common.types.id.TenantId.of(e.getTenantId()),
            e.getCreatedAt(),
            e.getCreatedBy(),
            AuditActorType.valueOf(e.getActorType()),
            UUID.fromString(e.getActorId()),
            AuditEntityType.valueOf(e.getEntityType()),
            UUID.fromString(e.getEntityId()),
            AuditAction.valueOf(e.getAction()),
            e.getDetails(),
            e.getIp(),
            e.getUserAgent());
    }
}

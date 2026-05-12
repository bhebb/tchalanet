package com.tchalanet.server.core.ledger.internal.infra.persistence;

import com.tchalanet.server.common.types.id.LedgerEntryId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerReference;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryJpaMapper {

    public LedgerEntry toDomain(LedgerEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new LedgerEntry(
            LedgerEntryId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            new LedgerReference(entity.getRefType(), entity.getRefId()),
            entity.getOperationType(),
            entity.getAmountCents(),
            entity.getCurrency(),
            entity.getDirection(),
            entity.getOccurredAt(),
            LedgerEntryId.nullableOf(entity.getReversalOfEntryId()),
            entity.getReason());
    }

    public LedgerEntryJpaEntity toEntity(LedgerEntry domain) {
        if (domain == null) {
            return null;
        }

        var entity = new LedgerEntryJpaEntity();
        entity.setId(domain.id().value());
        entity.setTenantId(domain.tenantId().value());
        entity.setRefType(domain.reference().type());
        entity.setRefId(domain.reference().id());
        entity.setOperationType(domain.operationType());
        entity.setAmountCents(domain.amountCents());
        entity.setCurrency(domain.currency());
        entity.setDirection(domain.direction());
        entity.setOccurredAt(domain.occurredAt());
        entity.setReversalOfEntryId(
            domain.reversalOfEntryId() == null ? null : domain.reversalOfEntryId().value());
        entity.setReason(domain.reason());

        return entity;
    }
}

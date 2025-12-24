package com.tchalanet.server.core.ledger.infra.persistence.adapter;

import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import com.tchalanet.server.core.ledger.infra.persistence.LedgerEntryJpaEntity;
import com.tchalanet.server.core.ledger.infra.persistence.repo.SpringLedgerEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerWriterPort, LedgerReaderPort {

    private final SpringLedgerEntryJpaRepository jpaRepository;

    @Override
    @Transactional
    public LedgerEntry append(LedgerEntry entry) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(entry.id(), "LedgerEntry.id must be set before append (generate UUID in domain)");

        var saved = jpaRepository.save(toEntity(entry));
        return toDomain(saved);
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity e) {
        return new LedgerEntry(
            e.getId(),
            e.getTenantId(),
            e.getRefType(),
            e.getRefId(),
            e.getAmount(),
            e.getDirection(),
            e.getOccurredAt()
        );
    }

    private LedgerEntryJpaEntity toEntity(LedgerEntry entry) {
        var ledgerEntryJpaEntity = new LedgerEntryJpaEntity();
        ledgerEntryJpaEntity.setId(entry.id());
        ledgerEntryJpaEntity.setTenantId(entry.tenantId());
        ledgerEntryJpaEntity.setRefType(entry.refType());
        ledgerEntryJpaEntity.setRefId(entry.refId());
        ledgerEntryJpaEntity.setAmount(entry.amount());
        ledgerEntryJpaEntity.setDirection(entry.direction());
        ledgerEntryJpaEntity.setOccurredAt(entry.occurredAt());
        return ledgerEntryJpaEntity;
    }


    @Override
    public void appendAll(List<LedgerEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        var entities = entries.stream()
            .map(this::toEntity)
            .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public BigDecimal getBalance(UUID tenantId) {
        return jpaRepository.computeBalance(tenantId);
    }

    @Override
    public List<LedgerEntry> findByTenant(UUID tenantId, Instant from, Instant to, int limit, int offset) {
        var entities = jpaRepository.findByTenantIdAndOccurredAtBetweenOrderByOccurredAtDesc(tenantId, from, to);
        // Note: the repo method doesn't use pageable, so we slice manually
        return entities.stream()
            .skip(offset)
            .limit(limit)
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<LedgerEntry> findByRef(UUID tenantId, LedgerRefType refType, UUID refId) {
        return jpaRepository.findByTenantIdAndRefTypeAndRefId(tenantId, refType, refId)
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public boolean existsByRef(UUID tenantId, LedgerRefType refType, UUID refId) {
        return jpaRepository.existsByTenantIdAndRefTypeAndRefId(tenantId, refType.name(), refId);
    }
}

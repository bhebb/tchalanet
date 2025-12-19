package com.tchalanet.server.core.ledger.infra.persistence;

import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerWriterPort {

  private final SpringLedgerEntryJpaRepository jpaRepository;

  @Override
  public LedgerEntry append(LedgerEntry entry) {
    LedgerEntryJpaEntity e = toEntity(entry);
    LedgerEntryJpaEntity saved = jpaRepository.save(e);
    return toDomain(saved);
  }

  private LedgerEntry toDomain(LedgerEntryJpaEntity e) {
    return LedgerEntry.load(e.getId(), e.getTenantId(), e.getRefType(), e.getRefId(), e.getAmount(), e.getDirection(), e.getOccurredAt());
  }

  private LedgerEntryJpaEntity toEntity(LedgerEntry entry) {
    LedgerEntryJpaEntity e = new LedgerEntryJpaEntity();
    e.setId(entry.getId());
    e.setTenantId(entry.getTenantId());
    e.setRefType(entry.getRefType());
    e.setRefId(entry.getRefId());
    e.setAmount(entry.getAmount());
    e.setDirection(entry.getDirection());
    e.setOccurredAt(entry.getOccurredAt());
    return e;
  }
}

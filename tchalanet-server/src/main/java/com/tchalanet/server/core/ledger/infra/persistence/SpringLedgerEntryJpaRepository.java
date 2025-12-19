package com.tchalanet.server.core.ledger.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringLedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
}


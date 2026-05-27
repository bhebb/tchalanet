package com.tchalanet.server.platform.reconciliation.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReconciliationRunJpaRepository extends JpaRepository<ReconciliationRunJpaEntity, UUID> {
}


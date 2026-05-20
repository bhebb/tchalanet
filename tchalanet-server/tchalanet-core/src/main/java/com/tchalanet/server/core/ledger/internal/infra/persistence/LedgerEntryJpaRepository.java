package com.tchalanet.server.core.ledger.internal.infra.persistence;

import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerRefType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    boolean existsByRefTypeAndRefIdAndOperationTypeAndDeletedAtIsNull(
        LedgerRefType refType,
        UUID refId,
        LedgerOperationType operationType);

    Optional<LedgerEntryJpaEntity> findByRefTypeAndRefIdAndOperationTypeAndDeletedAtIsNull(
        LedgerRefType refType,
        UUID refId,
        LedgerOperationType operationType);
}

package com.tchalanet.server.core.ledger.internal.infra.persistence;

import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.domain.model.LedgerReference;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaLedgerRepositoryAdapter implements LedgerWriterPort, LedgerReaderPort {

    private final LedgerEntryJpaRepository repository;
    private final LedgerEntryJpaMapper mapper;

    @Override
    public boolean appendIfAbsent(LedgerEntry entry) {
        try {
            repository.save(mapper.toEntity(entry));
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info(
                "Ledger entry already exists, skipping append: refType={} refId={} operationType={}",
                entry.reference().type(),
                entry.reference().id(),
                entry.operationType());
            return false;
        }
    }

    @Override
    public boolean existsByReferenceAndOperation(
        LedgerReference reference,
        LedgerOperationType operationType) {
        return repository.existsByRefTypeAndRefIdAndOperationTypeAndDeletedAtIsNull(
            reference.type(),
            reference.id(),
            operationType);
    }

    @Override
    public Optional<LedgerEntry> findByReferenceAndOperation(
        LedgerReference reference,
        LedgerOperationType operationType) {
        return repository
            .findByRefTypeAndRefIdAndOperationTypeAndDeletedAtIsNull(
                reference.type(),
                reference.id(),
                operationType)
            .map(mapper::toDomain);
    }
}

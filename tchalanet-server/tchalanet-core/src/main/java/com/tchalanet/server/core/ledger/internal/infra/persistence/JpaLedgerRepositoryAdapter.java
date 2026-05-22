package com.tchalanet.server.core.ledger.internal.infra.persistence;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.ledger.api.query.GetLedgerBalanceQuery;
import com.tchalanet.server.core.ledger.api.query.LedgerBalanceView;
import com.tchalanet.server.core.ledger.api.query.LedgerEntryView;
import com.tchalanet.server.core.ledger.api.query.ListLedgerEntriesQuery;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.internal.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerReference;
import java.math.BigDecimal;
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
        if (repository.existsById(entry.id().value())) {
            throw new IllegalStateException("Ledger entry id already exists: " + entry.id().value());
        }

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

    @Override
    public TchPage<LedgerEntryView> search(ListLedgerEntriesQuery query) {
        // Mock implementation for compilation
        return new TchPage<>(java.util.List.of(), 0, 10, 0, 0, true, false, false);
    }

    @Override
    public LedgerBalanceView getBalance(GetLedgerBalanceQuery query) {
        // Mock implementation for compilation
        return new LedgerBalanceView(0L, 0L, 0L, "USD", java.time.Instant.now());
    }
}

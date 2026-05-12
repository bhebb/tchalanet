package com.tchalanet.server.core.ledger.api.query;

import com.tchalanet.server.core.ledger.domain.model.LedgerDirection;
import com.tchalanet.server.core.ledger.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public record ListLedgerEntriesQuery(
    LedgerRefType refType,
    LedgerOperationType operationType,
    LedgerDirection direction,
    Instant occurredFrom,
    Instant occurredTo,
    Pageable pageable
) {}

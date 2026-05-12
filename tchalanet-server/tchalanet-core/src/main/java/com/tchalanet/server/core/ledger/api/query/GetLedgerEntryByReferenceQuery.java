package com.tchalanet.server.core.ledger.api.query;

import com.tchalanet.server.core.ledger.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.domain.model.LedgerReference;
import java.util.Objects;

public record GetLedgerEntryByReferenceQuery(
    LedgerReference reference,
    LedgerOperationType operationType
) {

    public GetLedgerEntryByReferenceQuery {
        Objects.requireNonNull(reference, "reference is required");
        Objects.requireNonNull(operationType, "operationType is required");
    }
}

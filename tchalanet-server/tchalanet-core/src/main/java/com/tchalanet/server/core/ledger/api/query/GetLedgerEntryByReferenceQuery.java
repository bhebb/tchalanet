package com.tchalanet.server.core.ledger.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerReference;
import java.util.Objects;
import java.util.Optional;

public record GetLedgerEntryByReferenceQuery(
    LedgerReference reference,
    LedgerOperationType operationType
) implements Query<Optional<LedgerEntryView>> {

    public GetLedgerEntryByReferenceQuery {
        Objects.requireNonNull(reference, "reference is required");
        Objects.requireNonNull(operationType, "operationType is required");
    }
}

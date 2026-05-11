package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;

public interface LedgerWriterPort {

    /**
     * Append-only write.
     *
     * <p>Must be idempotent using the DB unique constraint:
     * tenant_id + ref_type + ref_id + operation_type.
     */
    boolean appendIfAbsent(LedgerEntry entry);
}

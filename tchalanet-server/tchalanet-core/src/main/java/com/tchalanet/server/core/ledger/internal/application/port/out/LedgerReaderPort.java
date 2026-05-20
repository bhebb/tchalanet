package com.tchalanet.server.core.ledger.internal.application.port.out;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.ledger.api.query.GetLedgerBalanceQuery;
import com.tchalanet.server.core.ledger.api.query.LedgerBalanceView;
import com.tchalanet.server.core.ledger.api.query.LedgerEntryView;
import com.tchalanet.server.core.ledger.api.query.ListLedgerEntriesQuery;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.internal.domain.model.LedgerReference;

import java.util.Optional;

public interface LedgerReaderPort {

    boolean existsByReferenceAndOperation(
        LedgerReference reference,
        LedgerOperationType operationType);

    Optional<LedgerEntry> findByReferenceAndOperation(
        LedgerReference reference,
        LedgerOperationType operationType);

    TchPage<LedgerEntryView> search(ListLedgerEntriesQuery query);

    LedgerBalanceView getBalance(GetLedgerBalanceQuery query);

}

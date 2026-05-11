package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.ledger.application.query.model.GetLedgerBalanceQuery;
import com.tchalanet.server.core.ledger.application.query.model.LedgerBalanceView;
import com.tchalanet.server.core.ledger.application.query.model.LedgerEntryView;
import com.tchalanet.server.core.ledger.application.query.model.ListLedgerEntriesQuery;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerOperationType;
import com.tchalanet.server.core.ledger.domain.model.LedgerReference;

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

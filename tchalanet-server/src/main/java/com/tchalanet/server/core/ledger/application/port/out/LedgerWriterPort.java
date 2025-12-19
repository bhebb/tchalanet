package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;

public interface LedgerWriterPort {
  LedgerEntry append(LedgerEntry entry);
}


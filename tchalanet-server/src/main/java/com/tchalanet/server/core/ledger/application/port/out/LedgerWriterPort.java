package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import java.util.List;

public interface LedgerWriterPort {
  LedgerEntry append(LedgerEntry entry);

  void appendAll(List<LedgerEntry> entries);
}

package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.ReverseTransactionCommand;
import com.tchalanet.server.core.ledger.application.port.out.LedgerReaderPort;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntryFactory;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class ReverseTransactionCommandHandler
    implements VoidCommandHandler<ReverseTransactionCommand> {

  private final LedgerReaderPort ledgerReader;
  private final LedgerWriterPort ledgerWriter;

  @Override
  public void handle(ReverseTransactionCommand command) {
    var originals = ledgerReader.findByRef(command.tenantId(), command.refType(), command.refId());
    if (originals.isEmpty()) {
      throw ProblemRestException.notFound("No transactions found for reversal");
    }
    var reversals =
        originals.stream()
            .map(original -> LedgerEntryFactory.createReversal(original, command.occurredAt()))
            .collect(Collectors.toList());
    ledgerWriter.appendAll(reversals);
  }
}

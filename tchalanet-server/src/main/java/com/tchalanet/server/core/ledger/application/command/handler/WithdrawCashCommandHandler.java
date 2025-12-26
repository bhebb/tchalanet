package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.WithdrawCashCommand;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class WithdrawCashCommandHandler implements VoidCommandHandler<WithdrawCashCommand> {

  private final LedgerWriterPort ledgerWriter;

  @Override
  public void handle(WithdrawCashCommand command) {
    var entry =
        LedgerEntryFactory.createWithdraw(
            command.tenantId(), command.refId(), command.amount(), command.occurredAt());
    ledgerWriter.append(entry);
  }
}

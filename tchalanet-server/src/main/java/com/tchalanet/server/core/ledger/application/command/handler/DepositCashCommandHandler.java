package com.tchalanet.server.core.ledger.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.ledger.application.command.model.DepositCashCommand;
import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@Component
@RequiredArgsConstructor
public class DepositCashCommandHandler implements VoidCommandHandler<DepositCashCommand> {

    private final LedgerWriterPort ledgerWriter;

    @Override
    public void handle(DepositCashCommand command) {
        var entry = LedgerEntryFactory.createDeposit(command.tenantId(), command.refId(), command.amount(), command.occurredAt());
        ledgerWriter.append(entry);
    }
}

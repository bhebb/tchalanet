package com.tchalanet.server.core.terminal.internal.application.command.handler.sellerterminal;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.command.SetSellerTerminalCommissionRateCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SetSellerTerminalCommissionRateCommandHandler
    implements CommandHandler<SetSellerTerminalCommissionRateCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;

    @Override
    @TchTx
    public Void handle(SetSellerTerminalCommissionRateCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.sellerTerminalId());
        writer.save(terminal.updateCommissionRate(cmd.commissionRate()));
        return null;
    }
}

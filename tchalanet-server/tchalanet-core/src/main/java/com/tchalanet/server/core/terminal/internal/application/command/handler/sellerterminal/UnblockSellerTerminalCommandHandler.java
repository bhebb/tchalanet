package com.tchalanet.server.core.terminal.internal.application.command.handler.sellerterminal;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.command.UnblockSellerTerminalCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.sellerterminal.SellerTerminalWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class UnblockSellerTerminalCommandHandler
    implements CommandHandler<UnblockSellerTerminalCommand, Void> {

    private final SellerTerminalReaderPort reader;
    private final SellerTerminalWriterPort writer;
    private final Clock clock;

    @Override
    @TchTx
    public Void handle(UnblockSellerTerminalCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var unblocked = terminal.unblock(Instant.now(clock));
        writer.save(unblocked);
        return null;
    }
}

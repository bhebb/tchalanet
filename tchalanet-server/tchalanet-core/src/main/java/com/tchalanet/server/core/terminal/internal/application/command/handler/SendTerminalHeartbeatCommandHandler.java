package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.terminal.api.command.SendTerminalHeartbeatCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalCacheInvalidationPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SendTerminalHeartbeatCommandHandler
    implements VoidCommandHandler<SendTerminalHeartbeatCommand> {

    private final TerminalReaderPort reader;
    private final TerminalWriterPort writer;
    private final TerminalCacheInvalidationPort cacheInvalidation;

    @Override
    @TchTx
    public void handle(SendTerminalHeartbeatCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var updated = terminal.heartbeat(cmd.occurredAt());

        writer.save(updated);

        AfterCommit.run(() -> cacheInvalidation.evictTerminal(cmd.terminalId()));
    }
}

package com.tchalanet.server.core.terminal.internal.application.command.handler.lifecycle;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.api.command.LockTerminalCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalLockedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class LockTerminalCommandHandler implements VoidCommandHandler<LockTerminalCommand> {

    private final TerminalReaderPort reader;
    private final TerminalWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(LockTerminalCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var when = Instant.now(clock);
        writer.save(terminal.lock(cmd.performedBy(), cmd.reason(), when));

        var event =
            new TerminalLockedEvent(
                EventId.of(idGenerator.newUuid()),
                when,
                cmd.tenantId(),
                cmd.terminalId(),
                cmd.reason(),
                cmd.performedBy());
        AfterCommit.run(() -> publisher.publish(event));
    }
}

package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.application.command.model.UnlockTerminalCommand;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.event.TerminalUnlockedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class UnlockTerminalCommandHandler implements VoidCommandHandler<UnlockTerminalCommand> {

    private final TerminalReaderPort reader;
    private final TerminalWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UnlockTerminalCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var updated = terminal.unlock();

        if (updated == terminal) {
            return;
        }

        writer.save(updated);

        var now = Instant.now(clock);
        var event =
            new TerminalUnlockedEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                cmd.tenantId(),
                cmd.terminalId(),
                cmd.performedBy());

        AfterCommit.run(() -> publisher.publish(event));
    }
}

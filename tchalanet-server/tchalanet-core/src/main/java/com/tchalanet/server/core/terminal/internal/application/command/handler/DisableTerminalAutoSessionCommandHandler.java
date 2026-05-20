package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.api.command.DisableTerminalAutoSessionCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAutoSessionDisabledEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class DisableTerminalAutoSessionCommandHandler
    implements VoidCommandHandler<DisableTerminalAutoSessionCommand> {

    private final TerminalReaderPort reader;
    private final TerminalWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(DisableTerminalAutoSessionCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var updated = terminal.disableAutoSession();

        if (updated == terminal) {
            return;
        }

        writer.save(updated);

        var now = Instant.now(clock);
        var event =
            new TerminalAutoSessionDisabledEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                cmd.tenantId(),
                cmd.terminalId(),
                cmd.actorUserId());

        AfterCommit.run(() -> publisher.publish(event));
    }
}

package com.tchalanet.server.core.terminal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.application.command.model.RegisterTerminalCommand;
import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.event.TerminalRegisteredEvent;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class RegisterTerminalCommandHandler
    implements CommandHandler<RegisterTerminalCommand, TerminalId> {

    private final TerminalWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public TerminalId handle(RegisterTerminalCommand cmd) {
        var now = Instant.now(clock);
        var newId = TerminalId.of(idGenerator.newUuid());

        var terminal =
            Terminal.createNew(
                    newId,
                    cmd.tenantId(),
                    cmd.outletId(),
                    cmd.kind(),
                    cmd.label(),
                    cmd.inventoryTag(),
                    cmd.metadata(),
                    now)
                .register(now);

        writer.save(terminal);

        var event =
            new TerminalRegisteredEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                cmd.tenantId(),
                newId,
                cmd.outletId(),
                terminal.kind(),
                cmd.label(),
                cmd.actorUserId());

        AfterCommit.run(() -> publisher.publish(event));

        return newId;
    }
}

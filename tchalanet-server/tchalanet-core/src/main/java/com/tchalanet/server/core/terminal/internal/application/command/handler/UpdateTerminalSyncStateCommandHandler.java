package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.api.command.UpdateTerminalSyncStateCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalSyncStateUpdatedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class UpdateTerminalSyncStateCommandHandler
    implements VoidCommandHandler<UpdateTerminalSyncStateCommand> {

    private final TerminalReaderPort reader;
    private final TerminalWriterPort writer;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(UpdateTerminalSyncStateCommand cmd) {
        var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
        var previous = terminal.syncState();
        var now = Instant.now(clock);

        var updated = terminal.updateSyncState(cmd.newSyncState(), now);

        if (updated == terminal) {
            return;
        }

        writer.save(updated);

        var event =
            new TerminalSyncStateUpdatedEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                cmd.tenantId(),
                cmd.terminalId(),
                previous,
                cmd.newSyncState(),
                cmd.actorUserId());

        AfterCommit.run(() -> publisher.publish(event));
    }
}

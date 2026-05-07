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
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

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
    TerminalId newId = TerminalId.of(idGenerator.newUuid());
    Terminal t =
        Terminal.createNew(
                newId, cmd.tenantId(), cmd.outletId(), cmd.kind(), cmd.label(), cmd.inventoryTag(),
                cmd.metadata())
            .register(Instant.now(clock));
    writer.save(t);

    Instant when = Instant.now(clock);
    TerminalRegisteredEvent event =
        new TerminalRegisteredEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            newId,
            cmd.outletId(),
            t.kind(),
            cmd.label(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
    return newId;
  }
}

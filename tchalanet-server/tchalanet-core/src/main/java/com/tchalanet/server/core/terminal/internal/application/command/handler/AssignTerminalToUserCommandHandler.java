package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.api.command.AssignTerminalToUserCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalAssignedToUserEvent;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AssignTerminalToUserCommandHandler
    implements VoidCommandHandler<AssignTerminalToUserCommand> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(AssignTerminalToUserCommand cmd) {
    Terminal t = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    Terminal updated = t.assignToUser(cmd.userId());
    if (updated == t) return; // idempotent
    writer.save(updated);

    TerminalAssignedToUserEvent event =
        new TerminalAssignedToUserEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(clock),
            cmd.tenantId(),
            cmd.terminalId(),
            cmd.userId(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}

package com.tchalanet.server.core.terminal.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.api.command.UnregisterTerminalCommand;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.event.TerminalUnregisteredEvent;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UnregisterTerminalCommandHandler
    implements VoidCommandHandler<UnregisterTerminalCommand> {

  private final TerminalReaderPort reader;
  private final TerminalWriterPort writer;
  private final DomainEventPublisher publisher;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public void handle(UnregisterTerminalCommand cmd) {
    var terminal = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    var when = Instant.now(clock);
    writer.save(terminal.unregister(cmd.actorUserId(), when));

    var event =
        new TerminalUnregisteredEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            cmd.terminalId(),
            cmd.reason(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}

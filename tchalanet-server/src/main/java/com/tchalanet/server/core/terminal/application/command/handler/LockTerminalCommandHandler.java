package com.tchalanet.server.core.terminal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.terminal.application.command.model.LockTerminalCommand;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.port.out.TerminalWriterPort;
import com.tchalanet.server.core.terminal.domain.event.TerminalLockedEvent;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

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
    Terminal t = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    Instant when = Instant.now(clock);
    writer.save(t.lock(cmd.actorUserId(), cmd.reason(), when));

    TerminalLockedEvent event =
        new TerminalLockedEvent(
            EventId.of(idGenerator.newUuid()),
            when,
            cmd.tenantId(),
            cmd.terminalId(),
            cmd.reason(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}

package com.tchalanet.server.core.terminal.application.command.handler;

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
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

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
    Terminal t = reader.getRequired(cmd.tenantId(), cmd.terminalId());
    Terminal updated = t.unlock(Instant.now(clock));
    if (updated == t) return; // not locked, idempotent
    writer.save(updated);

    TerminalUnlockedEvent event =
        new TerminalUnlockedEvent(
            EventId.of(idGenerator.newUuid()),
            Instant.now(clock),
            cmd.tenantId(),
            cmd.terminalId(),
            cmd.actorUserId());
    AfterCommit.run(() -> publisher.publish(event));
  }
}

package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SessionId;
import com.tchalanet.server.core.session.application.command.model.OpenSessionCommand;
import com.tchalanet.server.core.session.application.port.out.SalesSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.domain.event.SessionOpenedEvent;
import com.tchalanet.server.core.session.domain.model.SalesSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OpenSessionCommandHandler implements CommandHandler<OpenSessionCommand, SalesSession> {

  private final SalesSessionReaderPort sessionReader;
  private final SalesSessionWriterPort sessionWriter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public SalesSession handle(OpenSessionCommand command) {
    // Refuse si déjà une session OPEN pour ce terminal (scope tenant + terminal)
    sessionReader
        .findOpenByTerminal(command.tenantId(), command.terminalId())
        .ifPresent(
            existing -> {
              throw new IllegalStateException(
                  "A POS session is already OPEN for tenant="
                      + command.tenantId()
                      + " and terminal="
                      + command.terminalId()
                      + " (sessionId="
                      + existing.id()
                      + ")");
            });

    var now = Instant.now(clock);

    var session =
        SalesSession.open(
            SessionId.of(idGenerator.newUuid()),
            command.tenantId(),
            command.outletId(),
            command.terminalId(),
            command.userId(),
            toCents(command.openingFloat()), // Convert BigDecimal to Long cents
            now);

    var saved = sessionWriter.save(session);

    var event =
        new SessionOpenedEvent(
           EventId.of(idGenerator.newUuid()),
            now,
            new com.tchalanet.server.common.types.id.TenantId(saved.tenantId().value()),
            saved.id(),
            saved.outletId(),
            saved.terminalId(),
            saved.userId(),
            saved.openingFloatCents());

    AfterCommit.run(() -> domainEventPublisher.publish(event));

    return saved;
  }

  private static long toCents(BigDecimal amount) {
    if (amount == null) return 0L;
    // stable conversion (assumes amount has max 2 decimals)
    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }
}

package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.session.application.command.model.CloseSessionCommand;
import com.tchalanet.server.core.session.application.port.out.PosSessionReaderPort;
import com.tchalanet.server.core.session.application.port.out.PosSessionWriterPort;
import com.tchalanet.server.core.session.domain.event.SessionClosedEvent;
import com.tchalanet.server.core.session.domain.model.PosSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CloseSessionCommandHandler implements CommandHandler<CloseSessionCommand, PosSession> {

  private final PosSessionReaderPort sessionReader;
  private final PosSessionWriterPort sessionWriter;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public PosSession handle(CloseSessionCommand command) {
    var existing =
        sessionReader
            .findById(command.sessionId().value())
            .orElseThrow(
                () -> new IllegalStateException("PosSession not found: " + command.sessionId()));

    // idempotent
    if (existing.closedAt() != null) {
      return existing;
    }

    var now = Instant.now(clock);
    var closed = existing.close(command.closingAmount(), now);
    var saved = sessionWriter.save(closed);

    Long closingAmountCents = toCentsOrNull(command.closingAmount());

    var event =
        new SessionClosedEvent(
            UUID.randomUUID(),
            now,
            new com.tchalanet.server.common.types.id.TenantId(saved.tenantId().uuid()),
            saved.id(),
            saved.outletId(),
            saved.userId(),
            saved.openedAt(),
            saved.closedAt(),
            closingAmountCents);

    AfterCommit.run(() -> domainEventPublisher.publish(event));

    return saved;
  }

  private static Long toCentsOrNull(BigDecimal amount) {
    if (amount == null) return null;
    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }
}

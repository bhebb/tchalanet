package com.tchalanet.server.core.session.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.exception.TchConflictException;
import com.tchalanet.server.common.exception.TchForbiddenException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.core.session.api.command.OpenSalesSessionCommand;
import com.tchalanet.server.core.session.api.command.OpenSalesSessionResult;
import com.tchalanet.server.core.session.internal.application.exception.SalesSessionAlreadyOpenException;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionOpeningContextReaderPort;
import com.tchalanet.server.core.session.internal.application.port.out.SalesSessionWriterPort;
import com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningEligibilityPolicy;
import com.tchalanet.server.core.session.api.event.SalesSessionOpenedEvent;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OpenSalesSessionCommandHandler implements CommandHandler<OpenSalesSessionCommand, OpenSalesSessionResult> {

  private final SalesSessionOpeningContextReaderPort openingContextReader;
  private final SalesSessionOpeningEligibilityPolicy eligibilityPolicy;
  private final SalesSessionWriterPort writer;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public OpenSalesSessionResult handle(OpenSalesSessionCommand command) {
      var now = Instant.now(clock);
      var businessDate = LocalDate.now(clock);

      var ctx = openingContextReader.loadForOpening(
          command.tenantId(),
          command.outletId(),
          command.terminalId(),
          command.openedBy(),
          businessDate);

      var eligibility = eligibilityPolicy.evaluate(ctx);

      if (!eligibility.canOpen()) {
          var code = eligibility.denialCode();
          var message = eligibility.message();

          if ("sales.session.already-open".equals(code)) {
              throw new SalesSessionAlreadyOpenException(
                  command.openedBy(),
                  eligibility.currentOpenSessionId().get());
          }

          if (code.contains("inactive") || code.contains("not-found")
              || code.contains("mismatch") || code.contains("not-bound")
              || code.contains("not-allowed") || code.contains("business-day")) {
              throw new TchForbiddenException(code, message);
          }

          throw new TchConflictException(code, message);
      }

      var sessionId = SalesSessionId.of(idGenerator.newUuid());
      var session =
          SalesSession.open(
              sessionId,
              command.tenantId(),
              command.outletId(),
              command.terminalId(),
              command.openedBy(),
              businessDate,
              now, command.openingFloatCents());

      var saved = writer.save(session);

      var event =
          new SalesSessionOpenedEvent(
              EventId.of(idGenerator.newUuid()),
              now,
              command.tenantId(),
              saved.id(),
              saved.outletId(),
              saved.terminalId(),
              command.openedBy());

      AfterCommit.run(() -> events.publish(event));

      return new OpenSalesSessionResult(saved.id(), now);
  }
}

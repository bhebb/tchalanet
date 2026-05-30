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
import com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningEligibility;
import com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningEligibilityPolicy;
import com.tchalanet.server.core.session.internal.application.service.time.SalesSessionBusinessDateResolver;
import com.tchalanet.server.core.session.api.event.SalesSessionOpenedEvent;
import com.tchalanet.server.core.session.internal.domain.model.SalesSession;
import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OpenSalesSessionCommandHandler implements CommandHandler<OpenSalesSessionCommand, OpenSalesSessionResult> {

  private final SalesSessionBusinessDateResolver businessDateResolver;
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

      var businessDate = businessDateResolver.resolve(
          command.tenantId(), command.outletId(), now);

      var openingContext = openingContextReader.loadForOpening(
          command.tenantId(),
          command.outletId(),
          command.terminalId(),
          command.openedBy(),
          businessDate);

      var eligibility = eligibilityPolicy.evaluate(openingContext);

      if (!eligibility.canOpen()) {
          throw toOpeningException(command, eligibility);
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

  private RuntimeException toOpeningException(
      OpenSalesSessionCommand command,
      SalesSessionOpeningEligibility eligibility
  ) {
      if ("sales.session.already-open".equals(eligibility.denialCode())) {
          var openSessionId = eligibility.currentOpenSessionId()
              .orElseThrow(() -> new IllegalStateException(
                  "sales.session.already-open without currentOpenSessionId"));

          return new SalesSessionAlreadyOpenException(command.openedBy(), openSessionId);
      }

      return switch (eligibility.denialKind()) {
          case FORBIDDEN -> new TchForbiddenException(
              eligibility.denialCode(),
              eligibility.message());
          case CONFLICT -> new TchConflictException(
              eligibility.denialCode(),
              eligibility.message());
      };
  }
}

package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.OpenSessionCommand;
import com.tchalanet.server.core.session.application.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.session.domain.event.SessionOpenedEvent;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OpenSessionCommandHandler implements CommandHandler<OpenSessionCommand, PosSession> {

      private final PosSessionRepositoryPort sessionRepository;
      private final DomainEventPublisher domainEventPublisher;
      private final Clock clock;

      @Override
      public PosSession handle(OpenSessionCommand command) {
          // Vérifier qu'il n'y a pas déjà une session OPEN pour ce tenant/terminal
          sessionRepository
              .findOpenByTerminal(command.tenantId(), command.terminalId())
              .ifPresent(
                  ignored -> {
                      throw new IllegalStateException(
                          "A POS session is already OPEN for tenant="
                              + command.tenantId()
                              + " and terminal="
                              + command.terminalId());
                  });

          Instant now = Instant.now(clock);

          PosSession session =
              PosSession.open(
                  UUID.randomUUID(),
                  command.tenantId(),
                  command.outletId(),
                  command.terminalId(),
                  command.userId(),
                  command.openingFloat(),   // Long en cents dans la commande
                  now
              );

          PosSession saved = sessionRepository.save(session);

          // publish domain event
          SessionOpenedEvent event =
              new SessionOpenedEvent(
                  UUID.randomUUID(),
                  now,
                  new TenantId(saved.tenantId()),
                  saved.outletId(),
                  saved.terminalId(),
                  saved.userId(),
                  saved.id(),
                  saved.openingFloatCents()
              );

          domainEventPublisher.publish(event);

          return saved;
      }
}

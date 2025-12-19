package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.draw.application.query.handler.GetDrawHandler;
import com.tchalanet.server.core.draw.application.query.model.GetDrawQuery;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.featureflags.application.annotation.FeatureFlagEnabled;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.application.ports.in.EvaluateLimitsForTicketUseCase;
import com.tchalanet.server.core.session.application.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.core.session.domain.model.PosSession;
import com.tchalanet.server.core.sales.application.command.model.CreateTicketCommand;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.application.port.out.TicketEventPublisherPort;
import com.tchalanet.server.core.sales.application.port.out.TicketNumberGeneratorPort;
import com.tchalanet.server.core.sales.application.port.out.TicketPublicCodeGeneratorPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateTicketCommandHandler implements CommandHandler<CreateTicketCommand, Ticket> {

  // --- Core Ports ---
  private final TicketWritterPort ticketRepository;
  private final TicketEventPublisherPort eventPublisher;
  private final DomainEventPublisher domainEventPublisher;

  // --- Generator Ports ---
  private final TicketNumberGeneratorPort numberGenerator;
  private final TicketPublicCodeGeneratorPort publicCodeGenerator;

  // --- Business Rule Ports ---
  private final PosSessionRepositoryPort posSessionPort;
  private final GetDrawHandler drawHandler;
  private final Clock clock;
  private final EvaluateLimitsForTicketUseCase evaluateLimitsForTicketUseCase;


  private Ticket createTicket(CreateTicketCommand command) {
    // Step 1: Session Validation
    PosSession session = validateSession(command.tenantId(), command.terminalId());

    // Step 2: Draw Resolution and Cutoff Validation
    var draw = resolveAndValidateDraw(command);

    // Step 3: Limit Policy Validation
    validateLimits(command, session.userId(), session.id());

    // --- If all validations pass, proceed with creation ---

    List<TicketLine> lines = calculateLines(command.lines());
    String ticketCode = numberGenerator.generate();
    String publicCode = publicCodeGenerator.generate();
    // todo add sessionId
    Instant now = Instant.now(clock);
    Ticket ticket =
        Ticket.create(
            command.tenantId(), command.terminalId(), null, draw.id(), ticketCode, publicCode, lines, now
            );

    Ticket savedTicket = ticketRepository.save(ticket);
    // keep existing port-based event for backward compatibility
    try {
      eventPublisher.publishTicketCreatedEvent(
          savedTicket.getId(), savedTicket.getTenantId(), session.id()); // Pass sessionId
    } catch (Exception e) {
      log.debug("legacy TicketEventPublisherPort.publishTicketCreatedEvent failed: {}", e.getMessage());
    }

    // publish new DomainEvent
    String firstGameCode = lines.isEmpty() ? "" : lines.get(0).gameCode();
    long totalStakeCents = savedTicket.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();
    TicketPlacedEvent domainEvent = new TicketPlacedEvent(
        UUID.randomUUID(),
        now,
        new TenantId(savedTicket.getTenantId()),
        savedTicket.getId(),
        null, // outletId not present on Ticket domain yet
        session.id(),
        savedTicket.getSessionId(),
        savedTicket.getDrawId(),
        firstGameCode,
        totalStakeCents,
        "USD" // currency unknown here; default placeholder
    );

    domainEventPublisher.publish(domainEvent);

    log.info(
        "Successfully created ticket {} for tenant {}",
        savedTicket.getPublicCode(),
        savedTicket.getTenantId());
    return savedTicket;
  }

  @Override
  @TchTx
  @RequiresPermission("ticket.create")
  @FeatureFlagEnabled(
      value = "ff.ticket.creation_enabled",
      tenantIdSpEL = "#command.tenantId") // Apply Feature Flag
  public Ticket handle(CreateTicketCommand command) {
    return createTicket(command);
  }

  private PosSession validateSession(UUID tenantId, UUID terminalId) {
    return posSessionPort
        .findOpenByTerminal(tenantId, terminalId)
        .orElseThrow(
            () ->
                new SecurityException(
                    "No open session for terminal: " + terminalId + " for tenant: " + tenantId));
  }

  private Draw resolveAndValidateDraw(CreateTicketCommand command) {
      var draw = drawHandler.handle(new GetDrawQuery(command.tenantId(), command.drawId()));

    // Use the draw's cutoff instant for cutoff comparison
    Instant cutoffInstant = draw.cutoffAt().toInstant();
    if (Instant.now(clock).isAfter(cutoffInstant)) {
      throw new IllegalStateException("Draw cutoff time has passed for draw: " + command.drawId());
    }
    return draw;
  }

  private void validateLimits(CreateTicketCommand command, UUID userId, UUID sessionId) {
    List<EvaluateLimitsForTicketUseCase.TicketLineInfo> lineInfos =
        command.lines().stream()
            .map(
                lineCmd ->
                    new EvaluateLimitsForTicketUseCase.TicketLineInfo(
                        lineCmd.gameCode(), lineCmd.selection(), lineCmd.stake()))
            .collect(Collectors.toList());

    EvaluateLimitsForTicketUseCase.LimitEvaluationCommand limitCommand =
        new EvaluateLimitsForTicketUseCase.LimitEvaluationCommand(
            command.tenantId(),
            command.terminalId(),
            userId, // Pass the user ID from the session
            sessionId, // Pass the session ID
            lineInfos);

    LimitEvaluationResult result = evaluateLimitsForTicketUseCase.evaluate(limitCommand);

    if (result.overallOutcome() == BreachOutcome.BLOCK) {
      throw new IllegalStateException(
          "Limit breach (BLOCK): " + String.join(", ", result.reasons()));
    } else if (result.overallOutcome() == BreachOutcome.WARN) {
      log.warn(
          "Limit breach (WARN) for tenant {}: {}",
          command.tenantId(),
          String.join(", ", result.reasons()));
    }
  }

  private List<TicketLine> calculateLines(List<CreateTicketCommand.LineCommand> lineCommands) {
    return lineCommands.stream()
        .map(
            lineCmd -> {
              BigDecimal odds = new BigDecimal("10.00"); // Placeholder via OddsCalculationPort
              BigDecimal potentialPayout = lineCmd.stake().multiply(odds);
              return new TicketLine(
                  lineCmd.gameCode(), lineCmd.selection(), lineCmd.stake(), odds, potentialPayout);
            })
        .collect(Collectors.toList());
  }
}

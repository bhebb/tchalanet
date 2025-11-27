package com.tchalanet.server.ticket.application;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.featureflags.application.annotation.FeatureFlagEnabled; // New import
import com.tchalanet.server.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.limitpolicy.domain.ports.in.EvaluateLimitsForTicketUseCase;
import com.tchalanet.server.pos.domain.model.PosSession;
import com.tchalanet.server.pos.domain.ports.out.PosSessionRepositoryPort;
import com.tchalanet.server.ticket.domain.model.Ticket;
import com.tchalanet.server.ticket.domain.model.TicketLine;
import com.tchalanet.server.ticket.domain.ports.in.CreateTicketUseCase;
import com.tchalanet.server.ticket.domain.ports.out.ClockPort;
import com.tchalanet.server.ticket.domain.ports.out.DrawResolutionPort;
import com.tchalanet.server.ticket.domain.ports.out.TicketEventPublisherPort;
import com.tchalanet.server.ticket.domain.ports.out.TicketNumberGeneratorPort;
import com.tchalanet.server.ticket.domain.ports.out.TicketPublicCodeGeneratorPort;
import com.tchalanet.server.ticket.domain.ports.out.TicketRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateTicketService implements CreateTicketUseCase {

  // --- Core Ports ---
  private final TicketRepositoryPort ticketRepository;
  private final TicketEventPublisherPort eventPublisher;

  // --- Generator Ports ---
  private final TicketNumberGeneratorPort numberGenerator;
  private final TicketPublicCodeGeneratorPort publicCodeGenerator;

  // --- Business Rule Ports ---
  private final PosSessionRepositoryPort posSessionPort;
  private final DrawResolutionPort drawResolutionPort;
  private final ClockPort clock;
  private final EvaluateLimitsForTicketUseCase evaluateLimitsForTicketUseCase;

  @Transactional
  @Override
  @RequiresPermission("ticket.create")
  @FeatureFlagEnabled(
      value = "ff.ticket.creation_enabled",
      tenantIdSpEL = "#command.tenantId") // Apply Feature Flag
  public Ticket createTicket(CreateTicketCommand command) {
    // Step 1: Session Validation
    PosSession session = validateSession(command.tenantId(), command.terminalId());

    // Step 2: Draw Resolution and Cutoff Validation
    DrawResolutionPort.Draw draw = resolveAndValidateDraw(command.drawId());

    // Step 3: Limit Policy Validation
    validateLimits(command, session.getUserId(), session.getId());

    // Step 4: Autonomy & Status (Placeholder)
    // UserAutonomy autonomy = userAutonomyPort.getForUser(command.userId());
    // TicketStatus initialStatus = determineInitialStatus(autonomy);

    // --- If all validations pass, proceed with creation ---

    List<TicketLine> lines = calculateLines(command.lines());
    String ticketCode = numberGenerator.generate();
    String publicCode = publicCodeGenerator.generate();
    // todo add sessionId
    Ticket ticket =
        Ticket.create(
            command.tenantId(), command.terminalId(), null, draw.id(), ticketCode, publicCode, lines
            // initialStatus // Pass status if determined by autonomy
            );

    Ticket savedTicket = ticketRepository.save(ticket);
    eventPublisher.publishTicketCreatedEvent(
        savedTicket.getId(), savedTicket.getTenantId(), session.getId()); // Pass sessionId

    log.info(
        "Successfully created ticket {} for tenant {}",
        savedTicket.getPublicCode(),
        savedTicket.getTenantId());
    return savedTicket;
  }

  private PosSession validateSession(UUID tenantId, UUID terminalId) {
    return posSessionPort
        .findOpenSessionByTerminal(tenantId, terminalId)
        .orElseThrow(
            () ->
                new SecurityException(
                    "No open session for terminal: " + terminalId + " for tenant: " + tenantId));
    // Here, you could implement the "auto-open" policy if needed.
  }

  private DrawResolutionPort.Draw resolveAndValidateDraw(UUID drawId) {
    DrawResolutionPort.Draw draw =
        drawResolutionPort
            .findById(drawId)
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + drawId));

    Instant cutoffTime = draw.scheduledAt().minusSeconds(draw.cutoffSec());
    if (clock.now().isAfter(cutoffTime)) {
      // Here, you could implement the "redirect to next draw" policy.
      throw new IllegalStateException("Draw cutoff time has passed for draw: " + drawId);
    }
    return draw;
  }

  private void validateLimits(CreateTicketCommand command, UUID userId, UUID sessionId) {
    // Convert LineCommand to TicketLineInfo for the LimitPolicy domain
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
    // If ALLOW, just proceed.
  }

  private List<TicketLine> calculateLines(List<LineCommand> lineCommands) {
    // This logic remains the same (fetching odds, calculating payout)
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

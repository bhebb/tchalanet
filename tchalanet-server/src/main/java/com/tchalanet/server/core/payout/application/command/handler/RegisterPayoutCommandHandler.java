package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.enums.OperationType;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.autonomy.application.service.ResolveAutonomyPolicyService;
import com.tchalanet.server.core.autonomy.application.service.model.AutonomyResolveRequest;
import com.tchalanet.server.core.limitpolicy.application.service.LimitPolicyRuntimeService;
import com.tchalanet.server.core.limitpolicy.application.query.model.LimitEvaluationView;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutResult;
import com.tchalanet.server.core.payout.application.port.out.PayoutApprovalPolicyPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.infra.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RegisterPayoutCommandHandler
    implements CommandHandler<RegisterPayoutCommand, RegisterPayoutResult> {

  private final PayoutReaderPort payoutReaderPort;
  private final PayoutWriterPort payoutWriterPort;
  private final TicketReaderPort ticketReaderPort;
  private final TicketWriterPort ticketWritterPort;
  private final PayoutApprovalPolicyPort approvalPolicy;
  private final DomainEventPublisher domainEventPublisher;
  private final Clock clock;
  private final LimitPolicyRuntimeService limitPolicyService;
  private final ResolveAutonomyPolicyService resolveAutonomyPolicyService;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public RegisterPayoutResult handle(RegisterPayoutCommand command) {
    Instant now = Instant.now(clock);
    // Load ticket
    Optional<Ticket> optTicket = ticketReaderPort.findWithLinesById(command.ticketId());
    if (optTicket.isEmpty()) {
      throw new IllegalStateException("Ticket not found: " + command.ticketId());
    }
    Ticket ticket = optTicket.get();

    // Validate ticket state
    if (ticket.getResultStatus() != com.tchalanet.server.common.types.enums.TicketResultStatus.WON) {
      throw new IllegalStateException("Ticket is not in RESULTED_WIN state: " + ticket.getId());
    }

    // payout amount: use ticket winning amount
    var payoutAmount = ticket.getWinningAmount();
    if (payoutAmount == null)
      throw new IllegalStateException("Ticket has no winning amount: " + ticket.getId());

    // Validate not already paid
    if (payoutReaderPort.findByTicketId(ticket.getId()).isPresent()) {
      throw new IllegalStateException("Payout already registered for ticket: " + ticket.getId());
    }

    // Limits and autonomy validation
    LimitEvaluationView limitView = validateLimitsAndAutonomy(command, ticket, payoutAmount);

    // Create payout and persist if allowed
    if (limitView.outcome() != BreachOutcome.BLOCK) {
      // Payout.createRequested expects amountCents and currency (domain uses cents)
      long amountCents = payoutAmount.movePointRight(2).longValue();
      Payout payout =
          Payout.createRequested(
              PayoutId.of(idGenerator.newUuid()),
              command.tenantId(),
              command.ticketId(),
              amountCents,
              "HTG",
              now);
      Payout saved = payoutWriterPort.save(payout);

      // Decide auto-approval via policy
      boolean autoApprove = approvalPolicy.autoApprove(command.tenantId(), payoutAmount);
      if (autoApprove) {
        // allow marking as paid from REQUESTED when auto-approved
        saved.markPaid(now, true);
        saved = payoutWriterPort.save(saved);

        // mark ticket paid and persist
        ticket.settle(now);
        ticketWritterPort.save(ticket);
      } else {
        // approve or leave requested based on workflow; for now mark as APPROVED
        saved.approve(now);
        saved = payoutWriterPort.save(saved);
      }

      // Publish event
      PayoutRegisteredEvent event =
          new PayoutRegisteredEvent(
              com.tchalanet.server.common.types.id.EventId.of(UUID.randomUUID()),
              now,
              command.tenantId(),
              saved.getId(),
              saved.getTicketId(),
              ticket.getSessionId(),
              java.math.BigDecimal.valueOf(saved.getAmountCents(), 2));

      AfterCommit.run(() -> domainEventPublisher.publish(event));

      log.info(
          "Payout {} registered with status={} for ticket {}",
          saved.getId(),
          saved.getStatus(),
          saved.getTicketId());

      List<LimitNotice> warnings =
          limitView.breaches().stream()
              .map(
                  d ->
                      new LimitNotice(
                          d.ruleKey().name(),
                          d.outcome(),
                          d.messageKey(),
                          d.appliedTarget(),
                          d.code(),
                          d.currentValue(),
                          d.limitValue()))
              .toList();

      return new RegisterPayoutResult(saved, "SUCCESS", warnings, null);
    } else {
      // BLOCK - pending approval
      UUID approvalRequestId = UUID.randomUUID(); // dummy
      return new RegisterPayoutResult(null, "PENDING_APPROVAL", List.of(), approvalRequestId);
    }
  }

  private LimitEvaluationView validateLimitsAndAutonomy(
      RegisterPayoutCommand command, Ticket ticket, java.math.BigDecimal payoutAmount) {
    Instant now = Instant.now(clock);

    // Build limit context for payout
    LimitScopeRef scope = new LimitScopeRef.TenantScope(command.tenantId());

    LimitContext context =
        new LimitContext(
            command.tenantId(),
            ticket.getDrawId(),
            null, // drawChannelId
            null, // agentId - not available
            ticket.getTerminalId(),
            null, // outletId - not available
            null, // zoneId
            List.of(), // rangeIds
            null, // gameCode
            OperationType.PAYOUT,
            scope,
            List.of(), // betLines - empty for payout
            payoutAmount, // use ticket winning amount
            0, // linesCount
            now,
            java.time.ZoneId.systemDefault());

    // Evaluate limits
    LimitEvaluationView view = limitPolicyService.evaluate(context);

    // Apply decision matrix V1
    if (view.outcome() == BreachOutcome.ALLOW) {
      // EXECUTE
    } else if (view.outcome() == BreachOutcome.WARN) {
      // EXECUTE + log
      log.warn(
          "Limit breach (WARN) tenantId={} details={}", command.tenantId(), view.breaches());
    } else if (view.outcome() == BreachOutcome.BLOCK) {
      // Resolve autonomy - agentId null, so perhaps use terminal or tenant
      var req = new AutonomyResolveRequest(null, ticket.getTerminalId(), null, now);
      var autonomyPolicy = resolveAutonomyPolicyService.resolve(req);
      if (!autonomyPolicy.requireApprovalOnBlock()) {
        // REJECT
        List<LimitNotice> notices =
            view.breaches().stream()
                .map(
                    d ->
                        new LimitNotice(
                            d.ruleKey().name(),
                            d.outcome(),
                            d.messageKey(),
                            d.appliedTarget(),
                            d.code(),
                            d.currentValue(),
                            d.limitValue()))
                .toList();
        throw ProblemRest.limitBlocked(
            "Limit breach blocked",
            OperationType.PAYOUT,
            notices,
            true,
            autonomyPolicy.approvalRole());
      }
      // else PENDING_APPROVAL - return result
    }

    return view;
  }
}

package com.tchalanet.server.core.payout.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.RegisterPayoutResult;
import com.tchalanet.server.core.payout.application.port.out.PayoutApprovalPolicyPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutRepositoryPort;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tchalanet.server.core.autonomy.domain.AutonomyResolver;
import com.tchalanet.server.core.limitpolicy.application.facade.LimitPolicyFacade;
import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitEvaluationResult;
import com.tchalanet.server.core.limitpolicy.domain.model.OperationType;
import com.tchalanet.server.core.tenant.domain.model.TenantId;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RegisterPayoutCommandHandler implements CommandHandler<RegisterPayoutCommand, RegisterPayoutResult> {

    private final PayoutRepositoryPort payoutRepository;
    private final TicketReaderPort ticketReaderPort;
    private final TicketWritterPort ticketWritterPort;
    private final PayoutApprovalPolicyPort approvalPolicy;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final LimitPolicyFacade limitPolicyFacade;
    private final AutonomyResolver autonomyResolver;

    @Override
    @TchTx
    public RegisterPayoutResult handle(RegisterPayoutCommand command) {
        Instant now = Instant.now(clock);
        // Load ticket
        Optional<Ticket> optTicket = ticketReaderPort.findWithLinesById(command.tenantId(), command.ticketId());
        if (optTicket.isEmpty()) {
            throw new IllegalStateException("Ticket not found: " + command.ticketId());
        }
        Ticket ticket = optTicket.get();

        // Validate ticket state
        if (ticket.getStatus() != com.tchalanet.server.core.sales.domain.model.TicketStatus.RESULTED_WIN) {
            throw new IllegalStateException("Ticket is not in RESULTED_WIN state: " + ticket.getId());
        }

        // Validate not already paid
        if (payoutRepository.findByTicketId(ticket.getId()).isPresent()) {
            throw new IllegalStateException("Payout already registered for ticket: " + ticket.getId());
        }

        // Limits and autonomy validation
        LimitEvaluationResult limitResult = validateLimitsAndAutonomy(command, ticket);

        // Create payout and persist if allowed
        if (limitResult.overallOutcome() != BreachOutcome.BLOCK) {
            Payout payout = Payout.createRequested(command.tenantId(), command.ticketId(), command.amount(), now);
            Payout saved = payoutRepository.save(payout);

            // Decide auto-approval via policy
            boolean autoApprove = approvalPolicy.autoApprove(command.tenantId(), command.amount());
            if (autoApprove) {
                saved.markPaid(now);
                saved = payoutRepository.save(saved);

                // mark ticket paid and persist
                ticket.markAsPaid(now);
                ticketWritterPort.save(ticket);
            } else {
                // approve or leave requested based on workflow; for now mark as APPROVED
                saved.approve(now);
                saved = payoutRepository.save(saved);
            }

            // Publish event
            PayoutRegisteredEvent event = new PayoutRegisteredEvent(
                UUID.randomUUID(),
                now,
                new TenantId(command.tenantId()),
                saved.getId(),
                saved.getTicketId(),
                ticket.getSessionId(),
                saved.getAmount());

            domainEventPublisher.publish(event);

            log.info("Payout {} registered with status={} for ticket {}", saved.getId(), saved.getStatus(), saved.getTicketId());

            List<LimitNotice> warnings = limitResult.details().stream()
                .map(d -> new LimitNotice(d.ruleKey(), d.outcome(), d.message(), d.targetApplied(), d.selectionKey(), d.currentValue(), d.limitValue()))
                .toList();

            return new RegisterPayoutResult(saved, "SUCCESS", warnings, null);
        } else {
            // BLOCK - pending approval
            UUID approvalRequestId = UUID.randomUUID(); // dummy
            return new RegisterPayoutResult(null, "PENDING_APPROVAL", List.of(), approvalRequestId);
        }
    }

    private LimitEvaluationResult validateLimitsAndAutonomy(RegisterPayoutCommand command, Ticket ticket) {
        Instant now = Instant.now(clock);

        // Build limit context for payout
        LimitContext context = new LimitContext(
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
            List.of(), // betLines - empty for payout
            command.amount(), // ticketStakeTotal - payout amount
            0, // linesCount
            now,
            java.time.ZoneId.systemDefault()
        );

        // Evaluate limits
        LimitEvaluationResult limitResult = limitPolicyFacade.evaluate(OperationType.PAYOUT, context);


        // Apply decision matrix V1
        if (limitResult.overallOutcome() == BreachOutcome.ALLOW) {
            // EXECUTE
        } else if (limitResult.overallOutcome() == BreachOutcome.WARN) {
            // EXECUTE + log
            log.warn("Limit breach (WARN) tenantId={} details={}", command.tenantId(), limitResult.details());
        } else if (limitResult.overallOutcome() == BreachOutcome.BLOCK) {
            // Resolve autonomy - agentId null, so perhaps use terminal or tenant
            var autonomyPolicy = autonomyResolver.resolve(command.tenantId(), null, ticket.getTerminalId(), null, now);
            if (!autonomyPolicy.requireApprovalOnBlock()) {
                // REJECT
                List<LimitNotice> notices = limitResult.details().stream()
                    .map(d -> new LimitNotice(d.ruleKey(), d.outcome(), d.message(), d.targetApplied(), d.selectionKey(), d.currentValue(), d.limitValue()))
                    .toList();
                throw ProblemRest.limitBlocked("Limit breach blocked", OperationType.PAYOUT, notices, true, autonomyPolicy.approvalRole());
            }
            // else PENDING_APPROVAL - return result
        }

        return limitResult;
    }
}

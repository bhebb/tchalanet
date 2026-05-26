package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.query.ValidateTerminalForOperationQuery;
import com.tchalanet.server.core.terminal.api.query.ValidatedTerminalOperationView;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.capability.TerminalCapabilityReaderPort;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.lifecycle.TerminalOperationPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ValidateTerminalForOperationQueryHandler
    implements QueryHandler<ValidateTerminalForOperationQuery, ValidatedTerminalOperationView> {

    private final TerminalReaderPort terminalReader;
    private final TerminalAssignmentReaderPort assignmentReader;
    private final TerminalDeviceBindingReaderPort bindingReader;
    private final TerminalCapabilityReaderPort capabilityReader;
    private final Clock clock;

    @Override
    public ValidatedTerminalOperationView handle(ValidateTerminalForOperationQuery q) {
        var terminal = terminalReader.getRequired(q.tenantId(), q.terminalId());

        if (!terminal.tenantId().equals(q.tenantId())) {
            throw ProblemRest.forbidden("terminal.tenant_mismatch");
        }

        if (!terminal.outletId().equals(q.outletId())) {
            throw ProblemRest.forbidden("terminal.outlet_mismatch");
        }

        if (!terminal.assignedTo(q.actorUserId())) {
            throw ProblemRest.forbidden("terminal.seller_not_assigned");
        }

        var assignment = assignmentReader.findActive(q.tenantId(), q.terminalId(), q.actorUserId())
            .orElseThrow(() -> ProblemRest.forbidden("terminal.assignment_missing"));

        if (terminal.locked()) {
            throw ProblemRest.forbidden("terminal.locked");
        }

        var capabilities = capabilityReader.findByTerminal(q.tenantId(), q.terminalId());
        var requiredCapability = TerminalOperationPolicy.requiredCapability(q.operation());
        requiredCapability.ifPresent(required -> requireCapability(capabilities, required));

        var bindingStatus = requiredCapability
            .map(_ignored -> requireActiveCompatibleBinding(q, terminal))
            .orElse(null);

        switch (q.operation()) {
            case SELL_TICKET, SELL_PHONE, PRINT_TICKET, REPRINT_TICKET, SCAN_TICKET -> {
                if (terminal.salesBlocked()) throw ProblemRest.forbidden("terminal.sales_blocked");
            }
            case PAYOUT_CLAIM -> {
                if (terminal.payoutBlocked()) throw ProblemRest.forbidden("terminal.payout_blocked");
            }
            case OFFLINE_GRANT -> {
                if (terminal.salesBlocked()) throw ProblemRest.forbidden("terminal.sales_blocked_for_offline_grant");
                if (terminal.offlineBlocked()) throw ProblemRest.forbidden("terminal.offline_blocked");
            }
            case OFFLINE_SYNC -> {
                // Receive for audit if not locked. If locked should be blocked above.
            }
            case CANCEL -> {
                // Compatibility path for current close-session flow.
            }
        }

        return new ValidatedTerminalOperationView(
            terminal.id(),
            terminal.outletId(),
            assignment.userId(),
            terminal.displayCode(),
            terminal.kind(),
            terminal.effectiveSurface(),
            terminal.lifecycleStatus(),
            terminal.syncState(),
            capabilities,
            bindingStatus,
            terminal.locked(),
            terminal.salesBlocked(),
            terminal.payoutBlocked(),
            terminal.offlineBlocked());
    }

    private static void requireCapability(Set<TerminalCapability> capabilities, TerminalCapability required) {
        if (!capabilities.contains(required)) {
            throw ProblemRest.forbidden("terminal.capability_missing." + required.name());
        }
    }

    private TerminalBindingStatus requireActiveCompatibleBinding(
        ValidateTerminalForOperationQuery query,
        com.tchalanet.server.core.terminal.internal.domain.model.Terminal terminal
    ) {
        var now = Instant.now(clock);
        var hasActiveCompatibleBinding = bindingReader.findActiveByTerminal(query.tenantId(), query.terminalId()).stream()
            .map(binding -> binding.expireIfDue(now))
            .anyMatch(binding -> binding.activeAt(now) && binding.compatibleWith(terminal.kind(), terminal.effectiveSurface()));

        if (!hasActiveCompatibleBinding) {
            throw ProblemRest.forbidden("terminal.binding_missing_or_incompatible");
        }
        return TerminalBindingStatus.ACTIVE;
    }
}

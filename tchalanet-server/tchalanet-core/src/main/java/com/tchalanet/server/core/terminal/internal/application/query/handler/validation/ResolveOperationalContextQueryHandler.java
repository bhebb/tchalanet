package com.tchalanet.server.core.terminal.internal.application.query.handler.validation;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.context.operational.OperationalContextHint;
import com.tchalanet.server.common.context.operational.OperationalContextSource;
import com.tchalanet.server.common.context.operational.OperationalContextTrust;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.terminal.api.query.ResolveOperationalContextQuery;
import com.tchalanet.server.core.terminal.internal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.assignment.TerminalAssignmentReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.service.binding.TerminalBindingCredentialHasher;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@UseCase
@RequiredArgsConstructor
public class ResolveOperationalContextQueryHandler
    implements QueryHandler<ResolveOperationalContextQuery, OperationalContextHint> {

    private final TerminalReaderPort terminalReader;
    private final TerminalAssignmentReaderPort assignmentReader;
    private final TerminalDeviceBindingReaderPort bindingReader;
    private final Clock clock;

    @Override
    public OperationalContextHint handle(ResolveOperationalContextQuery query) {
        var candidate = query.candidate();
        if (candidate == null || !candidate.hasPosFrame()) {
            return new OperationalContextHint(null, null, null, OperationalContextSource.NONE, OperationalContextTrust.NONE);
        }

        if (query.tenantId() == null || query.actorUserId() == null || candidate.terminalId() == null) {
            return weak(candidate);
        }

        if (StringUtils.isBlank(query.bindingCredential())) {
            return weak(candidate);
        }

        var terminal = terminalReader.findById(query.tenantId(), candidate.terminalId())
            .orElse(null);
        if (terminal == null || terminal.lifecycleStatus() != TerminalStatus.ACTIVE) {
            return weak(candidate);
        }

        if (assignmentReader.findActive(query.tenantId(), terminal.id(), query.actorUserId()).isEmpty()) {
            return weak(candidate);
        }

        var now = Instant.now(clock);
        var hasVerifiedBinding = bindingReader.findActiveByTerminal(query.tenantId(), terminal.id()).stream()
            .map(binding -> binding.expireIfDue(now))
            .filter(binding -> binding.activeAt(now))
            .filter(binding -> binding.compatibleWith(terminal.kind(), terminal.effectiveSurface()))
            .anyMatch(binding -> TerminalBindingCredentialHasher.matches(
                query.tenantId(),
                terminal.id(),
                query.bindingCredential(),
                binding.credentialHash()));

        if (!hasVerifiedBinding) {
            return weak(candidate);
        }

        return new OperationalContextHint(
            terminal.id(),
            terminal.outletId(),
            candidate.salesSessionId(),
            OperationalContextSource.SIGNED_DEVICE_BINDING,
            OperationalContextTrust.STRONG);
    }

    private static OperationalContextHint weak(OperationalContextHint candidate) {
        return new OperationalContextHint(
            candidate.terminalId(),
            candidate.outletId(),
            candidate.salesSessionId(),
            candidate.hasPosFrame() ? OperationalContextSource.CLIENT_CLAIM : OperationalContextSource.NONE,
            candidate.hasPosFrame() ? OperationalContextTrust.WEAK : OperationalContextTrust.NONE);
    }
}

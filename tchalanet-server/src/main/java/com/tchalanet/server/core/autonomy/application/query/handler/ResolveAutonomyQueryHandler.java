package com.tchalanet.server.core.autonomy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.ApprovalRole;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleReaderPort;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyPolicyRuleView;
import com.tchalanet.server.core.autonomy.application.query.model.ResolveAutonomyQuery;
import com.tchalanet.server.core.autonomy.application.query.model.ResolveAutonomyView;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class ResolveAutonomyQueryHandler implements QueryHandler<ResolveAutonomyQuery, ResolveAutonomyView> {

    private final AutonomyRuleReaderPort reader;
    private final Clock clock;

    @Override
    public ResolveAutonomyView handle(ResolveAutonomyQuery q) {
        var now = Instant.now(clock);
        AutonomyPolicyRule rule = reader.findEffective(
            q.tenantId(),
            q.outletId(),
            q.userId(),
            now
        ).orElse(null);

        var level = rule == null ? AutonomyLevel.PARTIAL : rule.level();
        var requireApprovalOnBlock = rule == null || rule.requireApprovalOnBlock();
        var approvalRole = rule == null ? ApprovalRole.OPERATOR : rule.approvalRole();

        var requiresApproval = switch (q.riskOutcome()) {
            case ALLOW, WARN -> false;
            case REQUIRE_APPROVAL -> true;
            case BLOCK -> switch (level) {
                case FULL -> false;
                case NONE -> true;
                case PARTIAL -> requireApprovalOnBlock;
            };
        };

        return new ResolveAutonomyView(
            requiresApproval,
            approvalRole.name(),
            level,
            rule == null ? "FALLBACK" : rule.targetType().name(),
            requireApprovalOnBlock,
            q.riskOutcome().name(),
            rule == null ? null : new AutonomyPolicyRuleView(
                rule.targetType().name(),
                rule.targetId() == null ? null : rule.targetId().toString(),
                rule.level(),
                rule.approvalRole() != null ? rule.approvalRole().name() : null,
                rule.requireApprovalOnBlock(),
                rule.enabled(),
                rule.startsAt(),
                rule.endsAt()));
    }
}

package com.tchalanet.server.core.autonomy.internal.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.AutonomyTargetType;
import com.tchalanet.server.core.autonomy.application.port.out.AutonomyRuleReaderPort;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyMeta;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyOverviewView;
import com.tchalanet.server.core.autonomy.application.query.model.AutonomyRule;
import com.tchalanet.server.core.autonomy.application.query.model.GetAutonomyOverviewQuery;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyPolicyRule;
import com.tchalanet.server.core.autonomy.domain.model.AutonomyTargetId;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetAutonomyOverviewQueryHandler
    implements QueryHandler<GetAutonomyOverviewQuery, AutonomyOverviewView> {

    private final AutonomyRuleReaderPort repository;
    private final TchContextResolver tchContextResolver;

    @Override
    public AutonomyOverviewView handle(GetAutonomyOverviewQuery query) {
        if (query.targetType() == null) {
            throw new IllegalArgumentException("targetType is required");
        }

        UUID effectiveTargetId =
            query.targetId() == null ? null : query.targetId().value();

        if (query.targetType() == AutonomyTargetType.TENANT && effectiveTargetId == null) {
            effectiveTargetId = tchContextResolver.currentOrThrow().tenantUuid();
        }
        // Read overview for the exact requested scope (do NOT perform hierarchy fallbacks)
        Optional<AutonomyPolicyRule> found = repository.findByTarget(query.targetType(), effectiveTargetId);

        AutonomyTargetId wrappedTargetId = effectiveTargetId == null ? null : AutonomyTargetId.of(effectiveTargetId);

        if (found.isPresent()) {
            AutonomyPolicyRule p = found.get();
            AutonomyRule rule =
                new AutonomyRule(
                    p.level(),
                    p.requireApprovalOnBlock(),
                    p.approvalRole(),
                    p.enabled(),
                    p.startsAt(),
                    p.endsAt());

            AutonomyMeta meta =
                new AutonomyMeta(
                    true,
                    p.deleted(),
                    p.id());

            return new AutonomyOverviewView(query.targetType(), wrappedTargetId, rule, meta);
        }

        // No configured rule for this exact scope
        AutonomyRule emptyRule = null;
        AutonomyMeta meta = new AutonomyMeta(false, false, null);
        return new AutonomyOverviewView(query.targetType(), wrappedTargetId, emptyRule, meta);
    }
}

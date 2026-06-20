package com.tchalanet.server.core.limitpolicy.internal.domain.resolver;

import com.tchalanet.server.core.limitpolicy.api.RuleKey;
import com.tchalanet.server.core.limitpolicy.api.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.EffectiveLimits;
import com.tchalanet.server.core.limitpolicy.internal.domain.model.LimitAssignment;

import java.util.EnumMap;
import java.util.List;

public class LimitResolver {

    public EffectiveLimits resolve(
        List<LimitAssignment> assignments,
        LimitContext ctx
    ) {
        var resolved = new EnumMap<RuleKey, EffectiveLimitRule>(RuleKey.class);

        for (var assignment : assignments) {
            if (!assignment.isActiveAt(ctx.now())) {
                continue;
            }

            var score = score(assignment.scope(), ctx);
            if (score < 0) {
                continue;
            }

            var existing = resolved.get(assignment.ruleKey());
            if (existing == null || score > score(existing.appliedScope(), ctx)) {
                resolved.put(
                    assignment.ruleKey(),
                    new EffectiveLimitRule(
                        assignment.ruleKey(),
                        assignment.onBreach(),
                        assignment.scope(),
                        assignment.id(),
                        assignment.params()));
            }
        }

        return new EffectiveLimits(resolved);
    }

    private int score(LimitScopeRef scope, LimitContext ctx) {
        return switch (scope) {
            case LimitScopeRef.AgentScope agent ->
                ctx.userId() != null && ctx.userId().equals(agent.userId()) ? 60 : -1;

            case LimitScopeRef.SellerTerminalScope sellerTerminalScope ->
                sellerTerminalScope.sellerTerminalId() != null && sellerTerminalScope.sellerTerminalId().equals(sellerTerminalScope.sellerTerminalId()) ? 60 : -1;

            case LimitScopeRef.DrawChannelScope channel ->
                ctx.drawChannelId() != null && ctx.drawChannelId().equals(channel.drawChannelId()) ? 30 : -1;

            case LimitScopeRef.TenantScope tenant -> ctx.tenantId().equals(tenant.tenantId()) ? 10 : -1;
            default -> throw new IllegalStateException("Unexpected value: " + scope);
        };
    }
}

package com.tchalanet.server.core.limitpolicy.application.query.handler.exposure;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.limitpolicy.application.port.out.assignment.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.exposure.ExposureAlertsReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.exposure.ExposureAlertItemView;
import com.tchalanet.server.core.limitpolicy.application.query.model.exposure.ExposureAlertsOverviewView;
import com.tchalanet.server.core.limitpolicy.application.query.model.exposure.GetExposureAlertsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.domain.model.EffectiveLimitRule;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetExposureAlertsOverviewQueryHandler
    implements QueryHandler<GetExposureAlertsOverviewQuery, ExposureAlertsOverviewView> {

    private final ExposureAlertsReaderPort alerts;
    private final LimitAssignmentReaderPort assignments;
    private final LimitResolver resolver;
    private final Clock clock;

    @Override
    public ExposureAlertsOverviewView handle(GetExposureAlertsOverviewQuery q) {
        var now = clock.instant();

        var ctx = contextForScope(q, now);

        var effectiveLimits = resolver.resolve(
            assignments.listActiveForTargets(ctx.scopes(), now),
            ctx);

        var maxStakeRule =
            effectiveLimits.rules().get(RuleKey.MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW);

        var maxPayoutRule =
            effectiveLimits.rules().get(RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW);

        var maxStake = valueCentsAsMoney(maxStakeRule);
        var maxPayout = valueCentsAsMoney(maxPayoutRule);

        var rows = new LinkedHashMap<String, ExposureAlertsReaderPort.Row>();

        alerts.topByStake(q.drawId(), q.scope(), q.limit())
            .forEach(row -> rows.put(key(row), row));

        alerts.topByPayout(q.drawId(), q.scope(), q.limit())
            .forEach(row -> rows.putIfAbsent(key(row), row));

        var items = rows.values().stream()
            .map(row -> toItem(row, maxStake, maxPayout))
            .toList();

        return new ExposureAlertsOverviewView(
            q.tenantId(),
            q.drawId(),
            scopeKey(q.scope()),
            items);
    }

    private LimitContext contextForScope(
        GetExposureAlertsOverviewQuery q,
        java.time.Instant now
    ) {
        OutletId outletId = null;
        UserId userId = null;
        DrawChannelId drawChannelId = null;

        switch (q.scope()) {
            case LimitScopeRef.OutletScope outlet -> outletId = outlet.outletId();
            case LimitScopeRef.AgentScope agent -> userId = agent.userId();
            case LimitScopeRef.DrawChannelScope channel -> drawChannelId = channel.drawChannelId();
            case LimitScopeRef.TenantScope ignored -> {
                // tenant already comes from q.tenantId()
            }
        }

        return new LimitContext(
            q.tenantId(),
            outletId,
            userId,
            q.drawId(),
            drawChannelId,
            now,
            List.of());
    }

    private ExposureAlertItemView toItem(
        ExposureAlertsReaderPort.Row row,
        BigDecimal maxStake,
        BigDecimal maxPayout
    ) {
        var stakeRatio = ratio(row.stakeTotal(), maxStake);
        var payoutRatio = ratio(row.potentialPayoutTotal(), maxPayout);

        return new ExposureAlertItemView(
            row.betType(),
            row.selectionKey(),
            row.stakeTotal(),
            row.potentialPayoutTotal(),
            row.salesCount(),
            maxStake,
            maxPayout,
            stakeRatio,
            payoutRatio);
    }

    private BigDecimal ratio(BigDecimal value, BigDecimal max) {
        if (value == null || max == null || max.signum() == 0) {
            return null;
        }

        return value.divide(max, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal valueCentsAsMoney(EffectiveLimitRule rule) {
        if (rule == null || rule.params() == null || rule.params().isNull()) {
            return null;
        }

        var node = rule.params().get("valueCents");

        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }

        return BigDecimal.valueOf(node.asLong(), 2);
    }

    private String key(ExposureAlertsReaderPort.Row row) {
        return row.betType().name() + ":" + row.selectionKey();
    }

    private String scopeKey(LimitScopeRef scope) {
        return switch (scope) {
            case LimitScopeRef.TenantScope tenant -> "TENANT:" + tenant.tenantId().value();
            case LimitScopeRef.DrawChannelScope channel -> "DRAW_CHANNEL:" + channel.drawChannelId().value();
            case LimitScopeRef.OutletScope outlet -> "OUTLET:" + outlet.outletId().value();
            case LimitScopeRef.AgentScope agent -> "AGENT:" + agent.userId().value();
        };
    }
}

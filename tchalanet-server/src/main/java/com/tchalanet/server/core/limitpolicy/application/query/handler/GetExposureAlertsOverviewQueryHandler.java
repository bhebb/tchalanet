package com.tchalanet.server.core.limitpolicy.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.core.limitpolicy.application.port.out.ExposureAlertsReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitAssignmentReaderPort;
import com.tchalanet.server.core.limitpolicy.application.port.out.LimitDefinitionReaderPort;
import com.tchalanet.server.core.limitpolicy.application.query.model.ExposureAlertItemView;
import com.tchalanet.server.core.limitpolicy.application.query.model.ExposureAlertsOverviewView;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetExposureAlertsOverviewQuery;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitContext;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetExposureAlertsOverviewQueryHandler implements QueryHandler<GetExposureAlertsOverviewQuery, ExposureAlertsOverviewView> {

    private final ExposureAlertsReaderPort alerts;
    private final LimitDefinitionReaderPort definitions;
    private final LimitAssignmentReaderPort assignments;
    private final LimitResolver resolver;

    @Override
    public ExposureAlertsOverviewView handle(GetExposureAlertsOverviewQuery q) {
        var ctx = new LimitContext(
            q.tenantId(), q.drawId(),
            null, null, null, null,
            null, List.of(), null,
            null, q.scope(),
            List.of(), BigDecimal.ZERO, 0,
            java.time.Instant.now(), java.time.ZoneId.of("UTC")
        );

        var eff = resolver.resolve(definitions.listActive(), assignments.listActive(q.tenantId()), ctx);
        var maxStake = eff.get(RuleKey.MAX_EXPOSURE_PER_SELECTION_PER_DRAW);
        var maxPayout = eff.get(RuleKey.MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW);

        var rowsByStake = alerts.topByStake(q.drawId(), q.scope(), q.limit());
        var rowsByPayout = alerts.topByPayout(q.drawId(), q.scope(), q.limit());
        var mergedRows = List.of(rowsByStake, rowsByPayout);
        var items = mergedRows.stream().flatMap(Collection::stream).map(r -> {
            var stakeRatio = (maxStake == null || maxStake.signum() == 0) ? null : r.stakeTotal().divide(maxStake, 4, java.math.RoundingMode.HALF_UP);
            var payoutRatio = (maxPayout == null || maxPayout.signum() == 0) ? null : r.potentialPayoutTotal().divide(maxPayout, 4, java.math.RoundingMode.HALF_UP);
            return new ExposureAlertItemView(
                r.betType(), r.selectionKey(),
                r.stakeTotal(), r.potentialPayoutTotal(), r.salesCount(),
                maxStake, maxPayout,
                stakeRatio, payoutRatio
            );
        }).toList();

        return new ExposureAlertsOverviewView(q.tenantId(), q.drawId(), q.scope().key(), items);
    }
}

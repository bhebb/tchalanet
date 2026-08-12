package com.tchalanet.server.features.tenantadmin.draw;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.limitpolicy.api.model.LimitScopeRef;
import com.tchalanet.server.core.limitpolicy.api.query.GetEffectiveLimitsForDrawQuery;
import com.tchalanet.server.core.limitpolicy.api.query.GetExposureAlertsOverviewQuery;
import com.tchalanet.server.core.sales.api.query.ListDrawTopSelectionsQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantAdminDrawOverviewService {

  private static final int TOP_SELECTIONS_LIMIT = 10;
  private static final int EXPOSURE_ALERTS_LIMIT = 10;

  private final QueryBus queryBus;

  public AdminDrawOverviewResponse getOverview(TchRequestContext ctx, DrawId drawId) {
    var tenantId = ctx.tenantIdRequired();

    DrawSummary draw = queryBus.ask(new GetDrawByIdQuery(drawId));

    var topSelectionsView =
        queryBus.ask(new ListDrawTopSelectionsQuery(tenantId, drawId, TOP_SELECTIONS_LIMIT));

    var effectiveLimitsView =
        queryBus.ask(new GetEffectiveLimitsForDrawQuery(tenantId, draw.drawChannelId()));

    var exposureAlertsView =
        queryBus.ask(
            new GetExposureAlertsOverviewQuery(
                tenantId,
                drawId,
                LimitScopeRef.drawChannel(draw.drawChannelId()),
                EXPOSURE_ALERTS_LIMIT));

    var drawInfo =
        new AdminDrawOverviewResponse.DrawInfo(
            draw.drawId().value(),
            draw.drawChannelCode(),
            draw.drawChannelLabel(),
            draw.drawDate(),
            draw.status(),
            draw.scheduledAt(),
            draw.openedAt(),
            draw.closedAt(),
            draw.result());

    List<AdminDrawOverviewResponse.TopSelectionItem> topSelections =
        topSelectionsView != null
            ? topSelectionsView.topSelections().stream()
                .map(
                    s ->
                        new AdminDrawOverviewResponse.TopSelectionItem(
                            s.rank(),
                            s.displaySelection(),
                            s.gameCode(),
                            s.betType(),
                            s.count(),
                            s.totalStakeCents()))
                .toList()
            : List.of();

    List<AdminDrawOverviewResponse.EffectiveLimitItem> effectiveLimits =
        effectiveLimitsView != null
            ? effectiveLimitsView.rules().stream()
                .map(
                    r ->
                        new AdminDrawOverviewResponse.EffectiveLimitItem(
                            r.ruleKey(), r.resolvedScope()))
                .toList()
            : List.of();

    List<AdminDrawOverviewResponse.ExposureAlertItem> exposureAlerts =
        exposureAlertsView != null
            ? exposureAlertsView.items().stream()
                .map(
                    item ->
                        new AdminDrawOverviewResponse.ExposureAlertItem(
                            item.betType().name(),
                            item.selectionKey(),
                            item.stakeTotal(),
                            item.salesCount(),
                            item.maxStakeExposureLimit(),
                            item.stakeRatio()))
                .toList()
            : List.of();

    return new AdminDrawOverviewResponse(drawInfo, topSelections, effectiveLimits, exposureAlerts);
  }
}

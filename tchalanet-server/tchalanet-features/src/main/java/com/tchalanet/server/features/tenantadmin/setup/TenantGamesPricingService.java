package com.tchalanet.server.features.tenantadmin.setup;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingQuery;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.GamePricingRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.LimitAssignmentRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.LimitsView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.PricingEntryRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.PricingView;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantGamesPricingService {

    private final TenantGameApi tenantGameApi;
    private final GameCatalog gameCatalog;
    private final QueryBus queryBus;

    public TenantGamesPricingView get(TenantId tenantId) {
        var tenantGames = tenantGameApi.listGames(tenantId);
        var limitAssignments = queryBus.ask(
            new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));
        var pricingOdds = queryBus.ask(new ListTenantPricingQuery(tenantId, null));

        // Group limits by ruleKey for easy lookup (all are tenant-level)
        var tenantLimitItems = limitAssignments.items();

        // Group pricing by gameCode
        var pricingByGame = pricingOdds.stream()
            .collect(Collectors.groupingBy(p -> p.gameCode().toUpperCase()));

        var rows = tenantGames.stream()
            .sorted(java.util.Comparator.comparingInt(
                com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView::displayOrder))
            .map(tg -> {
                var catalogGame = gameCatalog.findByCode(tg.gameCode());
                var catalogName = catalogGame.map(GameView::name).orElse(tg.gameCode());
                var category = catalogGame.map(GameView::category).orElse(null);

                var limitRows = tenantLimitItems.stream()
                    .filter(ListLimitAssignmentsView.Item::enabled)
                    .map(item -> new LimitAssignmentRow(
                        item.ruleKey(), item.enabled(), item.onBreach(), item.params()))
                    .toList();

                var gamePricingEntries = pricingByGame
                    .getOrDefault(tg.gameCode().toUpperCase(), List.of())
                    .stream()
                    .filter(p -> p.active())
                    .map(p -> new PricingEntryRow(
                        p.betType(),
                        p.betOption(),
                        p.pricingVariantCode().name(),
                        p.odds()))
                    .toList();

                return new GamePricingRow(
                    tg.gameCode(),
                    tg.tenantGameId(),
                    catalogName,
                    category,
                    tg.displayName(),
                    tg.displayOrder(),
                    tg.enabled(),
                    tg.visibleInPos(),
                    tg.minStake(),
                    tg.maxStake(),
                    new LimitsView(!limitRows.isEmpty(), limitRows),
                    new PricingView(!gamePricingEntries.isEmpty(), gamePricingEntries));
            })
            .toList();

        return new TenantGamesPricingView(rows);
    }
}

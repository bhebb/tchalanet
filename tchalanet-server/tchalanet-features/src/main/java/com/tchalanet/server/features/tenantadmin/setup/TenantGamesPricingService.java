package com.tchalanet.server.features.tenantadmin.setup;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.api.query.LimitScopeQueryRef;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsByScopeQuery;
import com.tchalanet.server.core.limitpolicy.api.query.ListLimitAssignmentsView;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.TenantPricingRuleView;
import com.tchalanet.server.core.pricing.api.query.ListTenantPricingRulesQuery;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.GamePricingRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.LimitAssignmentRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.LimitsView;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.PricingEntryRow;
import com.tchalanet.server.features.tenantadmin.setup.model.TenantGamesPricingView.PricingView;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantGamesPricingService {

  private static final Map<String, List<ExpectedPricingEntry>> EXPECTED_PRICING =
      Map.of(
          "HT_BOLET",
              List.of(
                  expected("MATCH_1_2D", "MATCH_1_2D", null),
                  expected("MATCH_2_2D", "MATCH_2_2D", null),
                  expected("MATCH_3_2D", "MATCH_3_2D", null)),
          "HT_MARYAJ", maryajExpectedPricing(),
          "HT_MARYAJ_GRATIS", maryajExpectedPricing(),
          "HT_MARYAJ_GRATUIT", maryajExpectedPricing(),
          "HT_LOTO3",
              List.of(
                  expected("LOTTO3_STRAIGHT", "LOTTO3_3D", (short) 1),
                  expected("LOTTO3_BOX_3_WAY", "LOTTO3_3D", (short) 2),
                  expected("LOTTO3_BOX_6_WAY", "LOTTO3_3D", (short) 2)),
          "HT_LOTO4",
              List.of(
                  expected("LOTTO4_STRAIGHT", "LOTTO4_PATTERN", (short) 1),
                  expected("LOTTO4_BOX_4_WAY", "LOTTO4_PATTERN", (short) 2),
                  expected("LOTTO4_BOX_6_WAY", "LOTTO4_PATTERN", (short) 2),
                  expected("LOTTO4_BOX_12_WAY", "LOTTO4_PATTERN", (short) 2),
                  expected("LOTTO4_BOX_24_WAY", "LOTTO4_PATTERN", (short) 2),
                  expected("LOTTO4_FRONT_PAIR", "LOTTO4_PATTERN", (short) 3),
                  expected("LOTTO4_BACK_PAIR", "LOTTO4_PATTERN", (short) 4)),
          "HT_LOTO5",
              List.of(
                  expected("LOTTO5_LOT1_LOT2", "LOTTO5_PATTERN", (short) 1),
                  expected("LOTTO5_LOT1_LOT3", "LOTTO5_PATTERN", (short) 2),
                  expected("LOTTO5_MIXED_1_2_3", "LOTTO5_PATTERN", (short) 3)));

  private final TenantGameApi tenantGameApi;
  private final GameCatalog gameCatalog;
  private final QueryBus queryBus;

  public TenantGamesPricingView get(TenantId tenantId) {
    var tenantGames = tenantGameApi.listGames(tenantId);
    var limitAssignments =
        queryBus.ask(new ListLimitAssignmentsByScopeQuery(LimitScopeQueryRef.tenant(tenantId)));
    var pricingRules = queryBus.ask(new ListTenantPricingRulesQuery(tenantId, null));

    // Group limits by ruleKey for easy lookup (all are tenant-level)
    var tenantLimitItems = limitAssignments.items();

    // Group pricing by gameCode
    var pricingByGame =
        pricingRules.stream().collect(Collectors.groupingBy(p -> p.gameCode().toUpperCase()));

    var rows =
        tenantGames.stream()
            .sorted(
                java.util.Comparator.comparingInt(
                    com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView
                        ::displayOrder))
            .map(
                tg -> {
                  var catalogGame = gameCatalog.findByCode(tg.gameCode());
                  var catalogName = catalogGame.map(GameView::name).orElse(tg.gameCode());
                  var category = catalogGame.map(GameView::category).orElse(null);

                  var limitRows =
                      tenantLimitItems.stream()
                          .filter(ListLimitAssignmentsView.Item::enabled)
                          .map(
                              item ->
                                  new LimitAssignmentRow(
                                      item.ruleKey(),
                                      item.enabled(),
                                      item.onBreach(),
                                      item.params()))
                          .toList();

                  var gamePricingEntries =
                      pricingEntries(
                          tg.gameCode(),
                          pricingByGame.getOrDefault(
                              tg.gameCode().toUpperCase(Locale.ROOT), List.of()));

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
                      new PricingView(
                          pricingConfigured(tg.gameCode(), gamePricingEntries),
                          gamePricingEntries));
                })
            .toList();

    return new TenantGamesPricingView(rows);
  }

  private static List<PricingEntryRow> pricingEntries(
      String gameCode, List<TenantPricingRuleView> tenantPricingRules) {
    var activeByVariant =
        tenantPricingRules.stream()
            .filter(TenantPricingRuleView::active)
            .collect(
                Collectors.toMap(
                    p -> p.pricingVariantCode().name(),
                    p -> p,
                    (first, ignored) -> first,
                    LinkedHashMap::new));

    var expected = EXPECTED_PRICING.getOrDefault(gameCode.toUpperCase(Locale.ROOT), List.of());
    var rows = new ArrayList<PricingEntryRow>();
    for (var entry : expected) {
      var actual = activeByVariant.remove(entry.pricingVariantCode().name());
      rows.add(
          new PricingEntryRow(
              entry.betType(),
              entry.betOption(),
              entry.pricingVariantCode().name(),
              actual == null ? null : actual.odds(),
              actual == null ? null : actual.payoutRuleType(),
              actual == null ? null : actual.fixedAmount()));
    }

    activeByVariant.values().stream()
        .map(
            p ->
                new PricingEntryRow(
                    p.betType(),
                    p.betOption(),
                    p.pricingVariantCode().name(),
                    p.odds(),
                    p.payoutRuleType(),
                    p.fixedAmount()))
        .forEach(rows::add);

    return List.copyOf(rows);
  }

  private static boolean pricingConfigured(String gameCode, List<PricingEntryRow> entries) {
    var expected = EXPECTED_PRICING.getOrDefault(gameCode.toUpperCase(Locale.ROOT), List.of());
    if (expected.isEmpty()) {
      return entries.stream().anyMatch(entry -> entry.odds() != null);
    }
    return entries.stream()
        .filter(
            entry ->
                expected.stream()
                    .anyMatch(
                        expectedEntry ->
                            expectedEntry
                                .pricingVariantCode()
                                .name()
                                .equals(entry.pricingVariantCode())))
        .allMatch(entry -> entry.odds() != null);
  }

  private static List<ExpectedPricingEntry> maryajExpectedPricing() {
    return List.of(
        expected("MARRIAGE_EXACT_ORDER", "MARRIAGE_2D2D", (short) 1),
        expected("MARRIAGE_REVERSE_ALLOWED", "MARRIAGE_2D2D", (short) 2));
  }

  private static ExpectedPricingEntry expected(
      String pricingVariantCode, String betType, Short betOption) {
    return new ExpectedPricingEntry(
        PricingVariantCode.valueOf(pricingVariantCode), betType, betOption);
  }

  private record ExpectedPricingEntry(
      PricingVariantCode pricingVariantCode, String betType, Short betOption) {}
}

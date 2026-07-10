package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.pricing.api.model.OddsSource;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameBetOptionConfigRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketLinePreparationServiceTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final SellerTerminalId SELLER_TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final UUID LINE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void resolvesEffectiveSellerTerminalOddsAndSnapshotsSettlementPayout() {
        var queryBus = new CapturingQueryBus(new BigDecimal("60"));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());

        var lines = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                "12",
                null,
                new BigDecimal("10.00"))),
            CurrencyCode.of("HTG"));

        assertThat(queryBus.captured).isEqualTo(new ResolveSellerTerminalOddsQuery(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            "HT_BOLET",
            PricingVariantCode.MATCH_1_2D,
            "MATCH_1_2D",
            null));
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().oddsSnapshot()).isEqualByComparingTo("60.0000");
        assertThat(lines.getFirst().settlementPayoutAmount().amount()).isEqualByComparingTo("600.00");
        assertThat(lines.getFirst().coverages()).hasSize(1);
        assertThat(lines.getFirst().coverages().getFirst().pricingVariantCode())
            .isEqualTo(PricingVariantCode.MATCH_1_2D);
        assertThat(lines.getFirst().coverages().getFirst().stakeAmount().amount())
            .isEqualByComparingTo("10.00");
        assertThat(lines.getFirst().coverages().getFirst().oddsSnapshot())
            .isEqualByComparingTo("60.0000");
        assertThat(lines.getFirst().coverages().getFirst().settlementPayoutSnapshot().amount())
            .isEqualByComparingTo("600.00");
        assertThat(lines.getFirst().selectionPolicySnapshot()).isEqualTo(SelectionPolicy.EXPLICIT_ONLY);
        assertThat(lines.getFirst().betOptionLabelSnapshot()).isNull();
    }

    @Test
    void resolvesLotto3BoxPricingVariantBeforeOddsLookup() {
        var queryBus = new CapturingQueryBus(new BigDecimal("10"));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());

        service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "112",
                (short) 2,
                new BigDecimal("10.00"))),
            CurrencyCode.of("HTG"));

        assertThat(queryBus.captured.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO3_BOX_3_WAY);

        service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "736",
                (short) 2,
                new BigDecimal("10.00"))),
            CurrencyCode.of("HTG"));

        assertThat(queryBus.captured.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO3_BOX_6_WAY);
    }

    @Test
    void exactPlusBoxCreatesTwoCoverageSnapshotsWithMinMaxSettlementPayout() {
        var queryBus = new CapturingQueryBus(Map.of(
            PricingVariantCode.LOTTO3_STRAIGHT, new BigDecimal("500"),
            PricingVariantCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")
        ));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());

        var line = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "123",
                (short) 3,
                new BigDecimal("20.00"))),
            CurrencyCode.of("HTG")).getFirst();

        assertThat(queryBus.capturedQueries)
            .extracting(ResolveSellerTerminalOddsQuery::pricingVariantCode)
            .containsExactly(
                PricingVariantCode.LOTTO3_STRAIGHT,
                PricingVariantCode.LOTTO3_BOX_6_WAY);
        assertThat(line.stakeAmount().amount()).isEqualByComparingTo("20.00");
        assertThat(line.settlementPayoutAmount().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.minSettlementPayout().amount()).isEqualByComparingTo("800.00");
        assertThat(line.maxSettlementPayout().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.totalSettlementPayout()).isNull();
        assertThat(line.coverages()).hasSize(2);
        assertThat(line.coverages())
            .extracting(coverage -> coverage.stakeAmount().amount())
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("10.00"));
        assertThat(line.coverages().get(0).settlementPayoutSnapshot().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.coverages().get(1).settlementPayoutSnapshot().amount()).isEqualByComparingTo("800.00");
        assertThat(line.selectionPolicySnapshot()).isEqualTo(SelectionPolicy.EXPLICIT_ONLY);
        assertThat(line.betOptionLabelSnapshot()).isEqualTo("Exact + Box");
    }

    @Test
    void exactPlusBoxDistributesOddCentStakeWithoutRejectingSale() {
        var queryBus = new CapturingQueryBus(Map.of(
            PricingVariantCode.LOTTO3_STRAIGHT, new BigDecimal("500"),
            PricingVariantCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")
        ));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());

        var line = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "123",
                (short) 3,
                new BigDecimal("10.01"))),
            CurrencyCode.of("HTG")).getFirst();

        assertThat(line.stakeAmount().amount()).isEqualByComparingTo("10.01");
        assertThat(line.coverages().get(0).stakeAmount().amount()).isEqualByComparingTo("5.01");
        assertThat(line.coverages().get(1).stakeAmount().amount()).isEqualByComparingTo("5.00");
        assertThat(line.coverages().stream()
                .map(coverage -> coverage.stakeAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("10.01");
        assertThat(line.coverages().get(0).settlementPayoutSnapshot().amount()).isEqualByComparingTo("2505.00");
        assertThat(line.coverages().get(1).settlementPayoutSnapshot().amount()).isEqualByComparingTo("400.00");
    }

    @Test
    void oddsSnapshotsAreNotRetroactive() {
        var queryBus = new CapturingQueryBus(new BigDecimal("60"));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());
        var input = new SellTicketLineInput(
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            "12",
            null,
            new BigDecimal("10.00"));

        var firstLine = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(input),
            CurrencyCode.of("HTG")).getFirst();

        queryBus.effectiveOdds = new BigDecimal("70");

        var secondLine = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(input),
            CurrencyCode.of("HTG")).getFirst();

        assertThat(firstLine.oddsSnapshot()).isEqualByComparingTo("60.0000");
        assertThat(firstLine.settlementPayoutAmount().amount()).isEqualByComparingTo("600.00");
        assertThat(secondLine.oddsSnapshot()).isEqualByComparingTo("70.0000");
        assertThat(secondLine.settlementPayoutAmount().amount()).isEqualByComparingTo("700.00");
    }

    @Test
    void implicitBestMatchUsesFullStakeForEachEnabledAlternative() {
        var queryBus = new CapturingQueryBus(Map.of(
            PricingVariantCode.LOTTO3_STRAIGHT, new BigDecimal("500"),
            PricingVariantCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")
        ));
        var service = service(queryBus, TenantGameApiStub.implicitBestMatch());

        var line = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "123",
                null,
                new BigDecimal("20.00"))),
            CurrencyCode.of("HTG")).getFirst();

        assertThat(queryBus.capturedQueries)
            .extracting(ResolveSellerTerminalOddsQuery::pricingVariantCode)
            .containsExactly(
                PricingVariantCode.LOTTO3_STRAIGHT,
                PricingVariantCode.LOTTO3_BOX_6_WAY);
        assertThat(queryBus.capturedQueries)
            .extracting(ResolveSellerTerminalOddsQuery::betOption)
            .containsExactly((short) 1, (short) 2);
        assertThat(line.betOption()).isNull();
        assertThat(line.settlementPayoutAmount().amount()).isEqualByComparingTo("10000.00");
        assertThat(line.minSettlementPayout().amount()).isEqualByComparingTo("1600.00");
        assertThat(line.maxSettlementPayout().amount()).isEqualByComparingTo("10000.00");
        assertThat(line.totalSettlementPayout()).isNull();
        assertThat(line.coverages()).hasSize(2);
        assertThat(line.coverages())
            .extracting(coverage -> coverage.stakeAmount().amount())
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("20.00"));
        assertThat(line.selectionPolicySnapshot()).isEqualTo(SelectionPolicy.IMPLICIT_BEST_MATCH);
        assertThat(line.betOptionLabelSnapshot()).isNull();
    }

    private static IdGenerator fixedIdGenerator() {
        return () -> LINE_ID;
    }

    private static TicketLinePreparationService service(
        QueryBus queryBus,
        TenantGameApi tenantGameApi
    ) {
        return new TicketLinePreparationService(
            new StubSelectionApi(),
            fixedIdGenerator(),
            queryBus,
            new TicketLineCoveragePlanner(tenantGameApi));
    }

    private static final class CapturingQueryBus implements QueryBus {
        private BigDecimal effectiveOdds;
        private Map<PricingVariantCode, BigDecimal> effectiveOddsByVariant = Map.of();
        private ResolveSellerTerminalOddsQuery captured;
        private final List<ResolveSellerTerminalOddsQuery> capturedQueries = new ArrayList<>();

        private CapturingQueryBus(BigDecimal effectiveOdds) {
            this.effectiveOdds = effectiveOdds;
        }

        private CapturingQueryBus(Map<PricingVariantCode, BigDecimal> effectiveOddsByVariant) {
            this.effectiveOdds = BigDecimal.ONE;
            this.effectiveOddsByVariant = effectiveOddsByVariant;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            captured = (ResolveSellerTerminalOddsQuery) query;
            capturedQueries.add(captured);
            var resolvedOdds = effectiveOddsByVariant.getOrDefault(
                captured.pricingVariantCode(),
                effectiveOdds);
            return (R) new SellerTerminalOddsResolutionView(
                captured.gameCode(),
                captured.pricingVariantCode(),
                captured.betType(),
                captured.betOption(),
                new BigDecimal("50"),
                resolvedOdds,
                resolvedOdds,
                OddsSource.SELLER_TERMINAL_OVERRIDE);
        }
    }

    private static final class StubSelectionApi implements SelectionApi {
        @Override
        public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
            return new Selection(SelectionKey.of(rawSelection), rawSelection);
        }

        @Override
        public Selection canonicalize(BetType betType, String rawSelection) {
            return canonicalize(betType, null, rawSelection);
        }

        @Override
        public SelectionValidationResult validate(BetType betType, Short betOption, String rawSelection) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public SelectionValidationResult validate(BetType betType, String rawSelection) {
            throw new UnsupportedOperationException("not used");
        }
    }

    private static final class TenantGameApiStub implements TenantGameApi {

        private final SelectionPolicy selectionPolicy;

        private TenantGameApiStub(SelectionPolicy selectionPolicy) {
            this.selectionPolicy = selectionPolicy;
        }

        static TenantGameApiStub explicitOnly() {
            return new TenantGameApiStub(SelectionPolicy.EXPLICIT_ONLY);
        }

        static TenantGameApiStub implicitBestMatch() {
            return new TenantGameApiStub(SelectionPolicy.IMPLICIT_BEST_MATCH);
        }

        @Override
        public TenantGameBetOptionConfigView getBetOptionConfig(TenantId tenantId, String gameCode) {
            return new TenantGameBetOptionConfigView(
                gameCode,
                List.of(new TenantBetTypeOptionConfigView(
                    BetType.LOTTO3_3D,
                    selectionPolicy,
                    null,
                    List.of(
                        new TenantBetOptionView((short) 1, "Exact", "Exact", true, true, 1),
                        new TenantBetOptionView((short) 2, "Box", "Box", true, true, 2),
                        new TenantBetOptionView((short) 3, "Exact + Box", "Exact + Box", false, true, 3)
                    ))));
        }

        @Override
        public List<TenantGameRefView> listGames(TenantId tenantId) {
            return List.of(new TenantGameRefView(
                TenantGameId.of(UUID.fromString("71000000-0000-0000-0000-000000000001")),
                null,
                GameCode.HT_LOTO3.name(),
                true,
                true,
                "Loto 3",
                1,
                new BigDecimal("1"),
                new BigDecimal("100")));
        }

        @Override
        public EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateTenantGameSettings(UpdateTenantGameSettingsRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TenantGameBetOptionConfigView updateBetOptionConfig(UpdateTenantGameBetOptionConfigRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ensureTenantGame(EnsureTenantGamesRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TenantGameRefView> findByTenantGameId(TenantId tenantId, TenantGameId tenantGameId) {
            throw new UnsupportedOperationException();
        }
    }
}

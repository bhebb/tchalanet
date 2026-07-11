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
import com.tchalanet.server.core.pricing.api.model.PayoutRuleType;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.pricing.api.model.SellerTerminalPayoutRuleResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalPayoutRuleQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementRuleCode;
import com.tchalanet.server.core.sales.api.model.settlement.SettlementTermSource;
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
    void resolvesEffectiveSellerTerminalOddsAndSnapshotsSettlementTermsWithoutPotentialPayout() {
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

        assertThat(queryBus.captured).isEqualTo(new ResolveSellerTerminalPayoutRuleQuery(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            "HT_BOLET",
            PricingVariantCode.MATCH_1_2D,
            "MATCH_1_2D",
            null));
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().settlementTermsSnapshot().terms()).hasSize(1);
        assertThat(lines.getFirst().settlementTermsSnapshot().terms().getFirst().ruleCode())
            .isEqualTo(SettlementRuleCode.MATCH_1_2D);
        assertThat(lines.getFirst().settlementTermsSnapshot().terms().getFirst().payoutBaseAmount())
            .isEqualByComparingTo("10.00");
        assertThat(lines.getFirst().selectionPolicySnapshot()).isEqualTo(SelectionPolicy.EXPLICIT_ONLY);
        assertThat(lines.getFirst().betOptionLabelSnapshot()).isNull();
        assertThat(lines.getFirst().settlementTermsSnapshot().terms().getFirst().source())
            .isEqualTo(SettlementTermSource.SELLER_TERMINAL_OVERRIDE);
        assertThat(lines.getFirst().settlementTermsSnapshot().terms().getFirst().payoutRule().type())
            .isEqualTo(PayoutRuleType.STAKE_MULTIPLIER);
        assertThat(lines.getFirst().settlementTermsSnapshot().terms().getFirst().payoutRule().multiplier())
            .isEqualByComparingTo("60");
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
    void exactPlusBoxSnapshotsTermsWithoutMinMaxPotentialPayout() {
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
            .extracting(ResolveSellerTerminalPayoutRuleQuery::pricingVariantCode)
            .containsExactly(
                PricingVariantCode.LOTTO3_STRAIGHT,
                PricingVariantCode.LOTTO3_BOX_6_WAY);
        assertThat(line.stakeAmount().amount()).isEqualByComparingTo("20.00");
        assertThat(line.settlementTermsSnapshot().terms()).hasSize(2);
        assertThat(line.settlementTermsSnapshot().terms())
            .extracting(term -> term.payoutBaseAmount())
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("10.00"));
        assertThat(line.settlementTermsSnapshot().terms())
            .extracting(term -> term.payoutRule().multiplier())
            .containsExactly(new BigDecimal("500"), new BigDecimal("80"));
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
        assertThat(line.settlementTermsSnapshot().terms().get(0).payoutBaseAmount()).isEqualByComparingTo("5.01");
        assertThat(line.settlementTermsSnapshot().terms().get(1).payoutBaseAmount()).isEqualByComparingTo("5.00");
        assertThat(line.settlementTermsSnapshot().terms().stream()
                .map(term -> term.payoutBaseAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo("10.01");
        assertThat(line.settlementTermsSnapshot().terms())
            .extracting(term -> term.payoutBaseAmount())
            .satisfiesExactly(
                amount -> assertThat(amount).isEqualByComparingTo("5.01"),
                amount -> assertThat(amount).isEqualByComparingTo("5.00"));
    }

    @Test
    void pricingRuleSnapshotsAreNotRetroactive() {
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

        assertThat(firstLine.settlementTermsSnapshot().terms().getFirst().payoutRule().multiplier())
            .isEqualByComparingTo("60");
        assertThat(secondLine.settlementTermsSnapshot().terms().getFirst().payoutRule().multiplier())
            .isEqualByComparingTo("70");
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
            .extracting(ResolveSellerTerminalPayoutRuleQuery::pricingVariantCode)
            .containsExactly(
                PricingVariantCode.LOTTO3_STRAIGHT,
                PricingVariantCode.LOTTO3_BOX_6_WAY);
        assertThat(queryBus.capturedQueries)
            .extracting(ResolveSellerTerminalPayoutRuleQuery::betOption)
            .containsExactly((short) 1, (short) 2);
        assertThat(line.betOption()).isNull();
        assertThat(line.settlementTermsSnapshot().terms()).hasSize(2);
        assertThat(line.settlementTermsSnapshot().terms())
            .extracting(term -> term.payoutBaseAmount())
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("20.00"));
        assertThat(line.selectionPolicySnapshot()).isEqualTo(SelectionPolicy.IMPLICIT_BEST_MATCH);
        assertThat(line.betOptionLabelSnapshot()).isNull();
        assertThat(line.settlementTermsSnapshot().terms())
            .extracting(term -> term.payoutRule().multiplier())
            .containsExactly(new BigDecimal("500"), new BigDecimal("80"));
    }

    @Test
    void fixedAmountRuleIsSnapshottedWithoutStakePayoutBase() {
        var queryBus = CapturingQueryBus.fixedAmount(new BigDecimal("2000"));
        var service = service(queryBus, TenantGameApiStub.explicitOnly());

        var line = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_LOTO3,
                BetType.LOTTO3_3D,
                "123",
                (short) 1,
                new BigDecimal("20.00"))),
            CurrencyCode.of("HTG")).getFirst();

        var term = line.settlementTermsSnapshot().terms().getFirst();
        assertThat(queryBus.captured.pricingVariantCode()).isEqualTo(PricingVariantCode.LOTTO3_STRAIGHT);
        assertThat(term.payoutRule().type()).isEqualTo(PayoutRuleType.FIXED_AMOUNT);
        assertThat(term.payoutRule().fixedAmount()).isEqualByComparingTo("2000");
        assertThat(term.payoutBaseAmount()).isNull();
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
            new TicketSettlementTermPlanner(tenantGameApi));
    }

    private static final class CapturingQueryBus implements QueryBus {
        private BigDecimal effectiveOdds;
        private Map<PricingVariantCode, BigDecimal> effectiveOddsByVariant = Map.of();
        private PayoutRuleType effectiveRuleType = PayoutRuleType.STAKE_MULTIPLIER;
        private BigDecimal effectiveFixedAmount;
        private ResolveSellerTerminalPayoutRuleQuery captured;
        private final List<ResolveSellerTerminalPayoutRuleQuery> capturedQueries = new ArrayList<>();

        private CapturingQueryBus(BigDecimal effectiveOdds) {
            this.effectiveOdds = effectiveOdds;
        }

        private CapturingQueryBus(Map<PricingVariantCode, BigDecimal> effectiveOddsByVariant) {
            this.effectiveOdds = BigDecimal.ONE;
            this.effectiveOddsByVariant = effectiveOddsByVariant;
        }

        private static CapturingQueryBus fixedAmount(BigDecimal amount) {
            var bus = new CapturingQueryBus(BigDecimal.ONE);
            bus.effectiveRuleType = PayoutRuleType.FIXED_AMOUNT;
            bus.effectiveFixedAmount = amount;
            return bus;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            captured = (ResolveSellerTerminalPayoutRuleQuery) query;
            capturedQueries.add(captured);
            var resolvedOdds = effectiveOddsByVariant.getOrDefault(
                captured.pricingVariantCode(),
                effectiveOdds);
            return (R) new SellerTerminalPayoutRuleResolutionView(
                captured.gameCode(),
                captured.pricingVariantCode(),
                captured.betType(),
                captured.betOption(),
                PayoutRuleType.STAKE_MULTIPLIER,
                new BigDecimal("50"),
                null,
                effectiveRuleType,
                effectiveRuleType == PayoutRuleType.STAKE_MULTIPLIER ? resolvedOdds : null,
                effectiveRuleType == PayoutRuleType.FIXED_AMOUNT ? effectiveFixedAmount : null,
                effectiveRuleType,
                effectiveRuleType == PayoutRuleType.STAKE_MULTIPLIER ? resolvedOdds : null,
                effectiveRuleType == PayoutRuleType.FIXED_AMOUNT ? effectiveFixedAmount : null,
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

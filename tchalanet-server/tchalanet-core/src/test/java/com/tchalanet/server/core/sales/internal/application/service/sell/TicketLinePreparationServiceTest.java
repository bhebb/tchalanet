package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.SellerTerminalId;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketLinePreparationServiceTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final SellerTerminalId SELLER_TERMINAL_ID =
        SellerTerminalId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final UUID LINE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Test
    void resolvesEffectiveSellerTerminalOddsAndSnapshotsPotentialPayout() {
        var queryBus = new CapturingQueryBus(new BigDecimal("60"));
        var service = new TicketLinePreparationService(new StubSelectionApi(), fixedIdGenerator(), queryBus);

        var lines = service.toTicketLines(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            List.of(new SellTicketLineInput(
                1,
                GameCode.HT_BOLET,
                BetType.MATCH_1_2D,
                "12",
                (short) 1,
                new BigDecimal("10.00"))),
            CurrencyCode.of("HTG"));

        assertThat(queryBus.captured).isEqualTo(new ResolveSellerTerminalOddsQuery(
            TENANT_ID,
            SELLER_TERMINAL_ID,
            "HT_BOLET",
            PricingVariantCode.MATCH_1_2D,
            "MATCH_1_2D",
            (short) 1));
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().oddsSnapshot()).isEqualByComparingTo("60.0000");
        assertThat(lines.getFirst().potentialPayoutAmount().amount()).isEqualByComparingTo("600.00");
        assertThat(lines.getFirst().coverages()).hasSize(1);
        assertThat(lines.getFirst().coverages().getFirst().pricingVariantCode())
            .isEqualTo(PricingVariantCode.MATCH_1_2D);
        assertThat(lines.getFirst().coverages().getFirst().stakeAmount().amount())
            .isEqualByComparingTo("10.00");
        assertThat(lines.getFirst().coverages().getFirst().oddsSnapshot())
            .isEqualByComparingTo("60.0000");
        assertThat(lines.getFirst().coverages().getFirst().potentialGainSnapshot().amount())
            .isEqualByComparingTo("600.00");
    }

    @Test
    void resolvesLotto3BoxPricingVariantBeforeOddsLookup() {
        var queryBus = new CapturingQueryBus(new BigDecimal("10"));
        var service = new TicketLinePreparationService(new StubSelectionApi(), fixedIdGenerator(), queryBus);

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
    void exactPlusBoxCreatesTwoCoverageSnapshotsWithMinMaxPotential() {
        var queryBus = new CapturingQueryBus(Map.of(
            PricingVariantCode.LOTTO3_STRAIGHT, new BigDecimal("500"),
            PricingVariantCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")
        ));
        var service = new TicketLinePreparationService(new StubSelectionApi(), fixedIdGenerator(), queryBus);

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
        assertThat(line.potentialPayoutAmount().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.minPotentialGain().amount()).isEqualByComparingTo("800.00");
        assertThat(line.maxPotentialGain().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.totalPotentialGain()).isNull();
        assertThat(line.coverages()).hasSize(2);
        assertThat(line.coverages())
            .extracting(coverage -> coverage.stakeAmount().amount())
            .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("10.00"));
        assertThat(line.coverages().get(0).potentialGainSnapshot().amount()).isEqualByComparingTo("5000.00");
        assertThat(line.coverages().get(1).potentialGainSnapshot().amount()).isEqualByComparingTo("800.00");
    }

    @Test
    void exactPlusBoxDistributesOddCentStakeWithoutRejectingSale() {
        var queryBus = new CapturingQueryBus(Map.of(
            PricingVariantCode.LOTTO3_STRAIGHT, new BigDecimal("500"),
            PricingVariantCode.LOTTO3_BOX_6_WAY, new BigDecimal("80")
        ));
        var service = new TicketLinePreparationService(new StubSelectionApi(), fixedIdGenerator(), queryBus);

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
        assertThat(line.coverages().get(0).potentialGainSnapshot().amount()).isEqualByComparingTo("2505.00");
        assertThat(line.coverages().get(1).potentialGainSnapshot().amount()).isEqualByComparingTo("400.00");
    }

    @Test
    void oddsSnapshotsAreNotRetroactive() {
        var queryBus = new CapturingQueryBus(new BigDecimal("60"));
        var service = new TicketLinePreparationService(new StubSelectionApi(), fixedIdGenerator(), queryBus);
        var input = new SellTicketLineInput(
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            "12",
            (short) 1,
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
        assertThat(firstLine.potentialPayoutAmount().amount()).isEqualByComparingTo("600.00");
        assertThat(secondLine.oddsSnapshot()).isEqualByComparingTo("70.0000");
        assertThat(secondLine.potentialPayoutAmount().amount()).isEqualByComparingTo("700.00");
    }

    private static IdGenerator fixedIdGenerator() {
        return () -> LINE_ID;
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
}

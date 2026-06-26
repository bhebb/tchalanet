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
import com.tchalanet.server.core.pricing.api.model.SellerTerminalOddsResolutionView;
import com.tchalanet.server.core.pricing.api.query.ResolveSellerTerminalOddsQuery;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import java.math.BigDecimal;
import java.util.List;
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
            "MATCH_1_2D",
            (short) 1));
        assertThat(lines).hasSize(1);
        assertThat(lines.getFirst().oddsSnapshot()).isEqualByComparingTo("60.0000");
        assertThat(lines.getFirst().potentialPayoutAmount().amount()).isEqualByComparingTo("600.00");
    }

    private static IdGenerator fixedIdGenerator() {
        return () -> LINE_ID;
    }

    private static final class CapturingQueryBus implements QueryBus {
        private final BigDecimal effectiveOdds;
        private ResolveSellerTerminalOddsQuery captured;

        private CapturingQueryBus(BigDecimal effectiveOdds) {
            this.effectiveOdds = effectiveOdds;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> R ask(Query<R> query) {
            captured = (ResolveSellerTerminalOddsQuery) query;
            return (R) new SellerTerminalOddsResolutionView(
                captured.gameCode(),
                captured.betType(),
                captured.betOption(),
                new BigDecimal("50"),
                effectiveOdds,
                effectiveOdds,
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

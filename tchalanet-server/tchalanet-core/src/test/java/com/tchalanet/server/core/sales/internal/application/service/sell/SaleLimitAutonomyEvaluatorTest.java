package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.api.query.DrawSummary;
import com.tchalanet.server.core.limitpolicy.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.api.query.EvaluateLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.api.query.LimitEvaluationView;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.application.service.sell.model.SalePolicyInput;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import com.tchalanet.server.core.session.api.model.ValidatedPosOperationContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SaleLimitAutonomyEvaluator")
class SaleLimitAutonomyEvaluatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    private static final DrawId DRAW_ID = DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private static final DrawChannelId DRAW_CHANNEL_ID = DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final Instant NOW = Instant.parse("2026-05-27T00:00:00Z");

    @Test
    @DisplayName("uses final promotion-adjusted payout risk for limit evaluation")
    void usesFinalPromotionAdjustedRisk() {
        var queryBus = new CapturingQueryBus();
        var evaluator = new SaleLimitAutonomyEvaluator(queryBus);

        var paidInput = new SellTicketLineInput(1, GameCode.HT_BOLET, BetType.MATCH_1_2D, "05", null, new BigDecimal("10"));
        var paidLine = line(1, "05", "10", "12.5", "125");
        var boostedLine = line(1, "05", "10", "20.0", "200");

        evaluator.evaluate(
            TENANT_ID,
            command(paidInput),
            pos(),
            draw(),
            new SalePolicyInput(List.of(paidInput), List.of(boostedLine), null),
            NOW
        );

        assertThat(queryBus.lastLimitQuery).isNotNull();
        var context = queryBus.lastLimitQuery.context();
        assertThat(context.lines()).hasSize(1);
        assertThat(context.totalStakeCents()).isEqualTo(1000L);
        assertThat(context.totalPotentialPayoutCents()).isEqualTo(20000L);
        assertThat(context.lines().getFirst().selectionKey()).isEqualTo(paidLine.selection().key().value());
    }

    private static SellTicketCommand command(SellTicketLineInput line) {
        return new SellTicketCommand(DRAW_ID, DRAW_CHANNEL_ID, HTG, List.of(line), null, List.of());
    }

    private static ValidatedPosOperationContext pos() {
        return new ValidatedPosOperationContext(
            TENANT_ID,
            UserId.of(UUID.fromString("30000000-0000-0000-0000-000000000001")),
            TerminalId.of(UUID.fromString("31000000-0000-0000-0000-000000000001")),
            OutletId.of(UUID.fromString("32000000-0000-0000-0000-000000000001")),
            SalesSessionId.of(UUID.fromString("33000000-0000-0000-0000-000000000001")),
            null,
            null,
            NOW
        );
    }

    private static DrawSummary draw() {
        return new DrawSummary(
            DRAW_ID,
            TENANT_ID,
            LocalDate.of(2026, 5, 27),
            DrawStatus.OPEN,
            NOW,
            NOW,
            null,
            NOW.plusSeconds(3600),
            null,
            null,
            DRAW_CHANNEL_ID,
            "NY-MID",
            "New York Midi",
            null,
            "America/Port-au-Prince",
            true,
            null,
            null,
            null,
            null,
            null,
            true,
            null
        );
    }

    private static TicketLine line(int lineNo, String selection, String stake, String odds, String payout) {
        return TicketLine.customerLine(
            TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-00000000000" + lineNo)),
            lineNo,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of(selection), selection),
            money(stake),
            new BigDecimal(odds),
            money(payout),
            null,
            TicketLineResultStatus.PENDING,
            money("0")
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }

    private static final class CapturingQueryBus implements QueryBus {
        private EvaluateLimitPolicyQuery lastLimitQuery;

        @SuppressWarnings("unchecked")
        @Override
        public <R> R ask(Query<R> query) {
            if (query instanceof EvaluateLimitPolicyQuery q) {
                lastLimitQuery = q;
                return (R) new LimitEvaluationView(BreachOutcome.ALLOW, List.of());
            }
            throw new AssertionError("Unexpected query: " + query);
        }
    }
}

package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.PromotionRuleId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.model.money.ChargePaidBy;
import com.tchalanet.server.core.sales.api.model.money.TicketCharge;
import com.tchalanet.server.core.sales.api.model.money.TicketChargeType;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SaleMoneyCalculator")
class SaleMoneyCalculatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private final SaleMoneyCalculator calculator = new SaleMoneyCalculator();

    @Test
    @DisplayName("normal paid ticket total is stake plus buyer charges")
    void normalPaidTicketTotalIncludesBuyerCharges() {
        var money = calculator.compute(
            List.of(line("10"), line("15")),
            List.of(new TicketCharge(TicketChargeType.BUYER_SMS, money("5"), ChargePaidBy.BUYER)),
            command()
        );

        assertThat(money.stake().amount()).isEqualByComparingTo("25");
        assertThat(money.totalBuyerCharges().amount()).isEqualByComparingTo("5");
        assertThat(money.total().amount()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("waived buyer charge is retained but excluded from buyer total")
    void waivedChargeIsExcludedFromBuyerTotal() {
        var waivedSms = new TicketCharge(TicketChargeType.BUYER_SMS, money("5"), ChargePaidBy.BUYER)
            .asWaived(PromotionRuleId.of(UUID.fromString("B1000000-0000-0000-0000-000000000001")));

        var money = calculator.compute(List.of(line("10")), List.of(waivedSms), command());

        assertThat(money.charges()).hasSize(1);
        assertThat(money.charges().getFirst().isWaived()).isTrue();
        assertThat(money.totalBuyerCharges().amount()).isEqualByComparingTo("0");
        assertThat(money.totalChargesAllPayers().amount()).isEqualByComparingTo("5");
        assertThat(money.total().amount()).isEqualByComparingTo("10");
    }

    private static SellTicketCommand command() {
        return new SellTicketCommand(null, null, HTG, List.of(), null, List.of());
    }

    private static TicketLine line(String stake) {
        return TicketLine.customerLine(
            TicketLineId.of(UUID.randomUUID()),
            1,
            GameCode.HT_BOLET,
            BetType.MATCH_1_2D,
            new Selection(SelectionKey.of("05"), "05"),
            money(stake),
            new BigDecimal("12.5"),
            money("125"),
            null,
            TicketLineResultStatus.PENDING,
            money("0")
        );
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}

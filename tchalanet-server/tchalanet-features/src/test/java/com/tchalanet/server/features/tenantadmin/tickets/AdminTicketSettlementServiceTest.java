package com.tchalanet.server.features.tenantadmin.tickets;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintLine;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource;
import com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource;
import com.tchalanet.server.core.sales.internal.application.service.result.SettlementExplanationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AdminTicketSettlementService line mapping")
class AdminTicketSettlementServiceTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");

    // QueryBus not needed for line mapping; the resolver primitive is real.
    private final AdminTicketSettlementService service =
        new AdminTicketSettlementService(null, new SettlementExplanationService());

    @Test
    @DisplayName("supported Loto 4 permuté line exposes the computed admin variant")
    void supportedLineResolved() {
        var view = service.toLineView(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

        assertThat(view.resolved()).isTrue();
        assertThat(view.variant()).isEqualTo("LOTTO4_BOX_24_WAY");
        assertThat(view.adminLabel()).isEqualTo("Permuté · 24-way");
        assertThat(view.commercialLabel()).isEqualTo("Désordre / Box");
        assertThat(view.error()).isNull();
    }

    @Test
    @DisplayName("Bòlèt option-less line resolves with lot label")
    void boletResolved() {
        var view = service.toLineView(line(GameCode.HT_BOLET, BetType.MATCH_1_2D, null, "36"));

        assertThat(view.resolved()).isTrue();
        assertThat(view.variant()).isEqualTo("MATCH_1_2D");
        assertThat(view.adminLabel()).isEqualTo("1er lot");
    }

    @Test
    @DisplayName("unsupported/legacy option is reported as unresolved, not thrown")
    void unsupportedLineUnresolved() {
        var view = service.toLineView(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 99, "1234"));

        assertThat(view.resolved()).isFalse();
        assertThat(view.variant()).isNull();
        assertThat(view.adminLabel()).isNull();
        assertThat(view.error()).isNotBlank();
        assertThat(view.betOption()).isEqualTo((short) 99);
    }

    private static TicketPrintLine line(GameCode game, BetType betType, Short betOption, String selection) {
        return new TicketPrintLine(
            1,
            game,
            betType,
            betOption,
            game.name(),
            selection,
            selection,
            new BigDecimal("12.5"),
            money("10"),
            TicketLineOrigin.CUSTOMER,
            TicketLinePricingSource.STANDARD,
            TicketLineSelectionSource.CUSTOMER_SELECTED,
            money("10"),
            null,
            null,
            null);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}

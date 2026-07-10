package com.tchalanet.server.core.sales.internal.application.service.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.ApprovalRequestId;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.TicketLineId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultProjection;
import com.tchalanet.server.core.pricing.api.model.PricingVariantCode;
import com.tchalanet.server.core.sales.api.model.coverage.SettlementPayoutMode;
import com.tchalanet.server.core.sales.api.model.money.TicketMoneyBreakdown;
import com.tchalanet.server.core.sales.api.model.origin.TicketSaleChannel;
import com.tchalanet.server.core.sales.api.model.status.TicketLineResultStatus;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketCodes;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketContext;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketIdentity;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLine;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.TicketLineCoverage;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.WinMode;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TicketWinningCalculator")
class TicketWinningCalculatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");
    private static final TicketLineId LINE_ID =
        TicketLineId.of(UUID.fromString("41000000-0000-0000-0000-000000000001"));

    private final TicketWinningCalculator calculator = new TicketWinningCalculator();

    @Nested
    @DisplayName("Bòlèt lot-specific matching")
    class Bolet {

        // lot1=736 (36), lot2=17, lot3=76
        private final DrawResultProjection draw = projection("736", "17", "76", null);

        @Test
        @DisplayName("MATCH_1_2D wins when selection equals lot1 last-2")
        void lot1Wins() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_1_2D, null, "36", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("MATCH_1_2D loses when 2D value only appears in another lot")
        void lot1DoesNotCross() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_1_2D, null, "17", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("MATCH_2_2D wins on lot2 only")
        void lot2Wins() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_2_2D, null, "17", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("MATCH_2_2D loses when value belongs to lot1")
        void lot2DoesNotCross() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_2_2D, null, "36", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("MATCH_3_2D wins on lot3 only")
        void lot3Wins() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_3_2D, null, "76", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("MATCH_3_2D loses when value belongs to lot1")
        void lot3DoesNotCross() {
            assertThat(status(GameCode.HT_BOLET, BetType.MATCH_3_2D, null, "36", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }
    }

    @Nested
    @DisplayName("Maryaj")
    class Maryaj {

        // ordered 2D = [36, 17, 76]
        private final DrawResultProjection draw = projection("36", "17", "76", null);

        @Test
        @DisplayName("exact order wins when numbers appear in played order")
        void exactOrderWins() {
            assertThat(status(GameCode.HT_MARYAJ, BetType.MARRIAGE_2D2D, (short) 1, "36-17", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("exact order loses when order is reversed")
        void exactOrderReversedLoses() {
            assertThat(status(GameCode.HT_MARYAJ, BetType.MARRIAGE_2D2D, (short) 1, "17-36", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("reverse allowed wins regardless of order")
        void reverseWins() {
            assertThat(status(GameCode.HT_MARYAJ, BetType.MARRIAGE_2D2D, (short) 2, "17-36", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("reverse allowed loses when one number is absent")
        void reverseMissingLoses() {
            assertThat(status(GameCode.HT_MARYAJ, BetType.MARRIAGE_2D2D, (short) 2, "36-99", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }
    }

    @Nested
    @DisplayName("Loto 3")
    class Lotto3 {

        // pick3 = lot1 = 736
        private final DrawResultProjection draw = projection("736", null, null, null);

        @Test
        @DisplayName("exact wins on identical order")
        void exactWins() {
            assertThat(status(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 1, "736", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("exact loses on wrong order")
        void exactWrongOrderLoses() {
            assertThat(status(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 1, "763", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("box 6-way wins on any permutation of 3 different digits")
        void box6WayWins() {
            assertThat(status(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 2, "367", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("box 3-way wins on permutation with a repeated digit")
        void box3WayWins() {
            // pick3 = 112
            var repeated = projection("112", null, null, null);
            assertThat(status(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 2, "211", repeated))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("box loses on different digits")
        void boxLoses() {
            assertThat(status(GameCode.HT_LOTO3, BetType.LOTTO3_3D, (short) 2, "789", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("exact plus box pays the best winning coverage")
        void exactPlusBoxPaysBestWinningCoverage() {
            var line = exactPlusBoxLoto3Line("123");

            var exactResult = calculator.computeLineResults(
                ticket(line, money("20")),
                projection("123", null, null, null));
            assertThat(exactResult.get(LINE_ID).status()).isEqualTo(TicketLineResultStatus.WON);
            assertThat(exactResult.get(LINE_ID).payoutAmount().amount()).isEqualByComparingTo("5000");

            var permutedResult = calculator.computeLineResults(
                ticket(line, money("20")),
                projection("321", null, null, null));
            assertThat(permutedResult.get(LINE_ID).status()).isEqualTo(TicketLineResultStatus.WON);
            assertThat(permutedResult.get(LINE_ID).payoutAmount().amount()).isEqualByComparingTo("800");
        }
    }

    @Nested
    @DisplayName("Loto 4")
    class Lotto4 {

        @Test
        @DisplayName("exact wins on identical order")
        void exactWins() {
            var draw = projection(null, null, null, "1234");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 1, "1234", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("exact loses on wrong order")
        void exactWrongOrderLoses() {
            var draw = projection(null, null, null, "1243");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 1, "1234", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("box 24-way wins on any permutation")
        void box24WayWins() {
            var draw = projection(null, null, null, "4321");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("box 12-way wins on permutation with one pair")
        void box12WayWins() {
            var draw = projection(null, null, null, "3211");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1123", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("box 6-way wins on permutation of two pairs")
        void box6WayWins() {
            var draw = projection(null, null, null, "2211");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1122", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("box 4-way wins on permutation with three identical digits")
        void box4WayWins() {
            var draw = projection(null, null, null, "2111");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1112", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("front pair wins when first two digits match")
        void frontPairWins() {
            var draw = projection(null, null, null, "1234");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 3, "12", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("front pair loses when digits are the back pair")
        void frontPairLoses() {
            var draw = projection(null, null, null, "1234");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 3, "34", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("back pair wins when last two digits match")
        void backPairWins() {
            var draw = projection(null, null, null, "1234");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 4, "34", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("back pair loses when digits are the front pair")
        void backPairLoses() {
            var draw = projection(null, null, null, "1234");
            assertThat(status(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 4, "12", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }
    }

    @Nested
    @DisplayName("Loto 5")
    class Lotto5 {

        // lot1=361, lot2=75, lot3=76
        private final DrawResultProjection draw = projection("361", "75", "76", null);

        @Test
        @DisplayName("lot1 + lot2 wins on 3D lot1 followed by 2D lot2")
        void lot1Lot2Wins() {
            assertThat(status(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 1, "36175", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("lot1 + lot2 loses on wrong tail")
        void lot1Lot2Loses() {
            assertThat(status(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 1, "36176", draw))
                .isEqualTo(TicketLineResultStatus.LOST);
        }

        @Test
        @DisplayName("lot1 + lot3 wins on 3D lot1 followed by 2D lot3")
        void lot1Lot3Wins() {
            assertThat(status(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 2, "36176", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("mixed wins on last-digit-lot1 + lot2 + lot3")
        void mixedWins() {
            assertThat(status(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 3, "17576", draw))
                .isEqualTo(TicketLineResultStatus.WON);
        }

        @Test
        @DisplayName("lot1 + lot2 loses when lot2 result is missing")
        void missingLot2Loses() {
            var incomplete = projection("361", null, "76", null);
            assertThat(status(GameCode.HT_LOTO5, BetType.LOTTO5_PATTERN, (short) 1, "36175", incomplete))
                .isEqualTo(TicketLineResultStatus.LOST);
        }
    }

    private TicketLineResultStatus status(
        GameCode gameCode,
        BetType betType,
        Short betOption,
        String played,
        DrawResultProjection draw
    ) {
        var ticket = ticket(gameCode, betType, betOption, played);
        var results = calculator.computeLineResults(ticket, draw);
        return results.get(LINE_ID).status();
    }

    private static Ticket ticket(GameCode gameCode, BetType betType, Short betOption, String played) {
        var line = TicketLine.customerLine(
            LINE_ID,
            1,
            gameCode,
            betType,
            new Selection(SelectionKey.of(played), played),
            money("10"),
            new BigDecimal("12.5"),
            money("125"),
            betOption,
            TicketLineResultStatus.PENDING,
            money("0"));

        return ticket(line, money("10"));
    }

    private static Ticket ticket(TicketLine line, Money stake) {
        return Ticket.place(
            new TicketIdentity(
                TicketId.of(UUID.fromString("40000000-0000-0000-0000-000000000001")),
                TenantId.of(UUID.fromString("20000000-0000-0000-0000-000000000001"))),
            new TicketContext(
                DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
                DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
                SellerTerminalId.of(UUID.fromString("91000000-0000-0000-0000-000000000001")),
                null,
                null),
            TicketCodes.from("TCK-123456-123456-ABC123-4", "ABCD-1234", "WXYZ-5678"),
            new TicketMoneyBreakdown(stake, List.of(), stake),
            List.of(line),
            TicketSaleChannel.POS_ONLINE,
            false,
            null,
            UserId.of(UUID.fromString("10000000-0000-0000-0000-000000000001")),
            NOW);
    }

    private static TicketLine exactPlusBoxLoto3Line(String played) {
        return new TicketLine(
            LINE_ID,
            1,
            GameCode.HT_LOTO3,
            BetType.LOTTO3_3D,
            new Selection(SelectionKey.of(played), played),
            money("20"),
            money("20"),
            new BigDecimal("500"),
            money("5000"),
            SettlementPayoutMode.RANGE_ALTERNATIVE,
            money("800"),
            money("5000"),
            null,
            List.of(
                new TicketLineCoverage(
                    PricingVariantCode.LOTTO3_STRAIGHT,
                    money("10"),
                    new BigDecimal("500"),
                    money("5000"),
                    WinMode.ALTERNATIVE),
                new TicketLineCoverage(
                    PricingVariantCode.LOTTO3_BOX_6_WAY,
                    money("10"),
                    new BigDecimal("80"),
                    money("800"),
                    WinMode.ALTERNATIVE)
            ),
            (short) 3,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLineOrigin.CUSTOMER,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLinePricingSource.STANDARD,
            com.tchalanet.server.core.sales.api.model.promotion.TicketLineSelectionSource.CUSTOMER_SELECTED,
            null,
            null,
            null,
            TicketLineResultStatus.PENDING,
            money("0")
        );
    }

    private static DrawResultProjection projection(String lot1, String lot2, String lot3, String lot4) {
        return new DrawResultProjection(
            DrawResultId.of(UUID.fromString("50000000-0000-0000-0000-000000000001")),
            "slot",
            NOW,
            lot1,
            lot2,
            lot3,
            lot4,
            null);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), HTG);
    }
}

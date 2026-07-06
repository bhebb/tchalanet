package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
import com.tchalanet.server.core.selection.api.model.SelectionValidationResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SaleCommandValidator — unsupported bet option is rejected at sale")
class SaleCommandValidatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");

    // SelectionApi is never reached for these cases (bet option validation throws first).
    private final SaleCommandValidator validator = new SaleCommandValidator(new UnreachableSelectionApi());

    @Test
    @DisplayName("unsupported option code is rejected with bet_option_out_of_range")
    void unsupportedOptionRejected() {
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 99, "1234"));

        assertThatThrownBy(() -> validator.validateCommand(command))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("sales.bet_option_out_of_range");
    }

    @Test
    @DisplayName("missing required option is rejected with bet_option_required")
    void missingRequiredOptionRejected() {
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "1234"));

        assertThatThrownBy(() -> validator.validateCommand(command))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("sales.bet_option_required");
    }

    @Test
    @DisplayName("option supplied for an option-less bet type is rejected")
    void optionNotAllowedRejected() {
        var command = command(line(GameCode.HT_BOLET, BetType.MATCH_1_2D, (short) 1, "36"));

        assertThatThrownBy(() -> validator.validateCommand(command))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("sales.bet_option_not_allowed");
    }

    @Test
    @DisplayName("a supported option passes bet option validation")
    void supportedOptionPasses() {
        // Reaches selection validation (stub returns a canonical selection) then succeeds.
        var validator = new SaleCommandValidator(new PassingSelectionApi());
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

        assertThat(command.lines()).hasSize(1);
        validator.validateCommand(command);
    }

    private static SellTicketCommand command(SellTicketLineInput line) {
        return new SellTicketCommand(
            DrawId.of(UUID.fromString("80000000-0000-0000-0000-000000000001")),
            DrawChannelId.of(UUID.fromString("90000000-0000-0000-0000-000000000001")),
            HTG,
            List.of(line),
            null,
            List.of());
    }

    private static SellTicketLineInput line(
        GameCode gameCode,
        BetType betType,
        Short betOption,
        String rawSelection
    ) {
        return new SellTicketLineInput(1, gameCode, betType, rawSelection, betOption, new BigDecimal("10"));
    }

    private static final class UnreachableSelectionApi implements SelectionApi {
        @Override
        public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
            throw new AssertionError("selection validation should not be reached");
        }

        @Override
        public Selection canonicalize(BetType betType, String rawSelection) {
            throw new AssertionError("selection validation should not be reached");
        }

        @Override
        public SelectionValidationResult validate(BetType betType, Short betOption, String rawSelection) {
            throw new AssertionError("selection validation should not be reached");
        }

        @Override
        public SelectionValidationResult validate(BetType betType, String rawSelection) {
            throw new AssertionError("selection validation should not be reached");
        }
    }

    private static final class PassingSelectionApi implements SelectionApi {
        @Override
        public Selection canonicalize(BetType betType, Short betOption, String rawSelection) {
            return new Selection(SelectionKeyValue(rawSelection), rawSelection);
        }

        @Override
        public Selection canonicalize(BetType betType, String rawSelection) {
            return canonicalize(betType, null, rawSelection);
        }

        @Override
        public SelectionValidationResult validate(BetType betType, Short betOption, String rawSelection) {
            return null;
        }

        @Override
        public SelectionValidationResult validate(BetType betType, String rawSelection) {
            return null;
        }

        private static com.tchalanet.server.core.selection.api.model.SelectionKey SelectionKeyValue(String v) {
            return com.tchalanet.server.core.selection.api.model.SelectionKey.of(v);
        }
    }
}

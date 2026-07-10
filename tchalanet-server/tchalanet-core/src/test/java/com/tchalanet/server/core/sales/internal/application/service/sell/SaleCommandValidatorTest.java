package com.tchalanet.server.core.sales.internal.application.service.sell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.core.selection.api.model.Selection;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SaleCommandValidator — unsupported bet option is rejected at sale")
class SaleCommandValidatorTest {

    private static final CurrencyCode HTG = CurrencyCode.of("HTG");
    private static final TenantId TENANT_ID = TenantId.of(UUID.fromString("70000000-0000-0000-0000-000000000001"));

    // SelectionApi is never reached for these cases (bet option validation throws first).
    private final SaleCommandValidator validator = new SaleCommandValidator(
        new UnreachableSelectionApi(),
        TenantGameApiStub.explicitOnly(true, true));

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
        var validator = new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, true));
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "1234"));

        validator.validateCommand(command);
        assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
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
        var validator = new SaleCommandValidator(new PassingSelectionApi(), TenantGameApiStub.explicitOnly(true, true));
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

        assertThat(command.lines()).hasSize(1);
        validator.validateCommand(command);
    }

    @Test
    @DisplayName("explicit-only hidden tenant option is rejected")
    void explicitOnlyHiddenOptionRejected() {
        var validator = new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.explicitOnly(true, false));
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

        validator.validateCommand(command);
        assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("sales.tenant_bet_option_not_visible_in_pos");
    }

    @Test
    @DisplayName("implicit-best-match accepts a missing client option")
    void implicitBestMatchAcceptsMissingOption() {
        var validator = new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.implicitBestMatch());
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, null, "1234"));

        validator.validateCommand(command);
        validator.validateTenantConfiguration(command, TENANT_ID);
    }

    @Test
    @DisplayName("implicit-best-match rejects a client supplied option")
    void implicitBestMatchRejectsClientOption() {
        var validator = new SaleCommandValidator(
            new PassingSelectionApi(),
            TenantGameApiStub.implicitBestMatch());
        var command = command(line(GameCode.HT_LOTO4, BetType.LOTTO4_PATTERN, (short) 2, "1234"));

        validator.validateCommand(command);
        assertThatThrownBy(() -> validator.validateTenantConfiguration(command, TENANT_ID))
            .isInstanceOf(ProblemRestException.class)
            .hasMessageContaining("sales.bet_option_not_allowed");
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

    private static final class TenantGameApiStub implements TenantGameApi {

        private final SelectionPolicy selectionPolicy;
        private final boolean optionEnabled;
        private final boolean optionVisibleInPos;

        private TenantGameApiStub(
            SelectionPolicy selectionPolicy,
            boolean optionEnabled,
            boolean optionVisibleInPos
        ) {
            this.selectionPolicy = selectionPolicy;
            this.optionEnabled = optionEnabled;
            this.optionVisibleInPos = optionVisibleInPos;
        }

        static TenantGameApiStub explicitOnly(boolean optionEnabled, boolean optionVisibleInPos) {
            return new TenantGameApiStub(SelectionPolicy.EXPLICIT_ONLY, optionEnabled, optionVisibleInPos);
        }

        static TenantGameApiStub implicitBestMatch() {
            return new TenantGameApiStub(SelectionPolicy.IMPLICIT_BEST_MATCH, true, true);
        }

        @Override
        public TenantGameBetOptionConfigView getBetOptionConfig(TenantId tenantId, String gameCode) {
            return new TenantGameBetOptionConfigView(
                gameCode,
                List.of(new TenantBetTypeOptionConfigView(
                    BetType.LOTTO4_PATTERN,
                    selectionPolicy,
                    null,
                    List.of(new TenantBetOptionView(
                        (short) 2,
                        "Box",
                        "Box",
                        optionEnabled,
                        optionVisibleInPos,
                        1)))));
        }

        @Override
        public List<TenantGameRefView> listGames(TenantId tenantId) {
            return List.of(new TenantGameRefView(
                TenantGameId.of(UUID.fromString("71000000-0000-0000-0000-000000000001")),
                null,
                GameCode.HT_LOTO4.name(),
                true,
                true,
                "Loto 4",
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

package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SaleCommandValidator {

    private final SelectionApi selectionApi;
    private final TenantGameApi tenantGameApi;

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    public void validateCommand(SellTicketCommand command) {
        if (command.drawId() == null) {
            throw ProblemRest.badRequest("sales.draw_required");
        }
        if (command.drawChannelId() == null) {
            throw ProblemRest.badRequest("sales.draw_channel_required");
        }
        if (command.currency() == null) {
            throw ProblemRest.badRequest("sales.currency_required");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw ProblemRest.badRequest("sales.lines_required");
        }
        var distinctLineNumbers = command.lines().stream()
            .map(SellTicketLineInput::lineNumber)
            .distinct().count();
        if (distinctLineNumbers != command.lines().size()) {
            throw ProblemRest.badRequest("sales.duplicate_line_number");
        }
        for (var line : command.lines()) {
            validateLine(line);
        }
    }

    public void validateTenantConfiguration(SellTicketCommand command, TenantId tenantId) {
        var gamesByCode = new HashMap<String, TenantGameRefView>();
        tenantGameApi.listGames(tenantId).forEach(game -> gamesByCode.put(game.gameCode(), game));

        var configsByGame = new HashMap<String, TenantGameBetOptionConfigView>();
        for (var line : command.lines()) {
            var gameCode = line.gameCode().name();
            var tenantGame = gamesByCode.get(gameCode);
            if (tenantGame == null) {
                throw ProblemRest.badRequest("sales.tenant_game_not_configured");
            }
            if (!tenantGame.enabled()) {
                throw ProblemRest.badRequest("sales.tenant_game_disabled");
            }
            if (!tenantGame.visibleInPos()) {
                throw ProblemRest.badRequest("sales.tenant_game_not_visible_in_pos");
            }
            validateTenantStake(line.stakeAmount(), tenantGame);

            var optionConfig = configsByGame.computeIfAbsent(
                gameCode,
                code -> tenantGameApi.getBetOptionConfig(tenantId, code));
            var betTypeConfig = requireTenantBetTypeConfig(optionConfig, line.betType());
            validateTenantBetOption(line.betOption(), line.betType(), betTypeConfig);
        }
    }

    private void validateLine(SellTicketLineInput line) {
        if (line.lineNumber() <= 0) throw ProblemRest.badRequest("sales.invalid_line_number");
        if (line.gameCode() == null) throw ProblemRest.badRequest("sales.game_required");
        if (line.betType() == null) throw ProblemRest.badRequest("sales.bet_type_required");
        if (!line.gameCode().supports(line.betType()))
            throw ProblemRest.badRequest("sales.unsupported_bet_type");
        if (line.rawSelection() == null || line.rawSelection().isBlank())
            throw ProblemRest.badRequest("sales.selection_required");
        if (line.stakeAmount() == null || line.stakeAmount().signum() <= 0)
            throw ProblemRest.badRequest("sales.invalid_stake_amount");
        validateBetOption(line);
        validateSelection(line);
    }

    private void validateSelection(SellTicketLineInput line) {
        try {
            selectionApi.canonicalize(line.betType(), line.betOption(), line.rawSelection());
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.badRequest("sales.selection_invalid");
        }
    }

    private void validateBetOption(SellTicketLineInput line) {
        if (line.betOption() == null) {
            return;
        }
        try {
            BetOption.from(line.betType(), line.betOption());
        } catch (IllegalArgumentException ex) {
            if (!line.betType().requiresOption()) {
                throw ProblemRest.badRequest("sales.bet_option_not_allowed");
            }
            throw ProblemRest.badRequest("sales.bet_option_out_of_range");
        }
    }

    private static void validateTenantStake(BigDecimal stakeAmount, TenantGameRefView tenantGame) {
        if (tenantGame.minStake() != null && stakeAmount.compareTo(tenantGame.minStake()) < 0) {
            throw ProblemRest.badRequest("sales.stake_below_tenant_min");
        }
        if (tenantGame.maxStake() != null && stakeAmount.compareTo(tenantGame.maxStake()) > 0) {
            throw ProblemRest.badRequest("sales.stake_above_tenant_max");
        }
    }

    private static TenantBetTypeOptionConfigView requireTenantBetTypeConfig(
        TenantGameBetOptionConfigView optionConfig,
        BetType betType
    ) {
        return optionConfig.betTypes().stream()
            .filter(config -> config.betType() == betType)
            .findFirst()
            .orElseThrow(() -> ProblemRest.badRequest("sales.tenant_bet_type_not_configured"));
    }

    private static void validateTenantBetOption(
        Short betOption,
        BetType betType,
        TenantBetTypeOptionConfigView betTypeConfig
    ) {
        if (!betType.requiresOption()) {
            return;
        }
        if (betTypeConfig.selectionPolicy() == SelectionPolicy.IMPLICIT_BEST_MATCH) {
            if (betOption != null) {
                throw ProblemRest.badRequest("sales.bet_option_not_allowed");
            }
            return;
        }
        if (betOption == null) {
            throw ProblemRest.badRequest("sales.bet_option_required");
        }

        var option = requireTenantBetOption(betOption, betTypeConfig);
        if (!option.enabled()) {
            throw ProblemRest.badRequest("sales.tenant_bet_option_disabled");
        }
        if (!option.visibleInPos()) {
            throw ProblemRest.badRequest("sales.tenant_bet_option_not_visible_in_pos");
        }
    }

    private static TenantBetOptionView requireTenantBetOption(
        Short betOption,
        TenantBetTypeOptionConfigView betTypeConfig
    ) {
        return betTypeConfig.options().stream()
            .filter(option -> Objects.equals(option.code(), betOption))
            .findFirst()
            .orElseThrow(() -> ProblemRest.badRequest("sales.tenant_bet_option_not_configured"));
    }
}

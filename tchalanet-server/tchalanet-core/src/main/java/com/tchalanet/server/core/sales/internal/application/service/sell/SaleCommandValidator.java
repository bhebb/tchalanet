package com.tchalanet.server.core.sales.internal.application.service.sell;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketLineInput;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.selection.api.SelectionApi;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SaleCommandValidator {

  private final SelectionApi selectionApi;
  private final TenantGameApi tenantGameApi;
  private final DrawChannelCatalog drawChannelCatalog;

  // -------------------------------------------------------------------------
  // Validation
  // -------------------------------------------------------------------------

  public void validateCommand(SellTicketCommand command) {
    if (command.drawId() == null) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_REQUIRED);
    }
    if (command.drawChannelId() == null) {
      throw ProblemRest.of(SalesErrorCodes.DRAW_CHANNEL_REQUIRED);
    }
    if (command.currency() == null) {
      throw ProblemRest.of(SalesErrorCodes.CURRENCY_REQUIRED);
    }
    if (command.lines() == null || command.lines().isEmpty()) {
      throw ProblemRest.of(SalesErrorCodes.LINES_REQUIRED);
    }
    var distinctLineNumbers =
        command.lines().stream().map(SellTicketLineInput::lineNumber).distinct().count();
    if (distinctLineNumbers != command.lines().size()) {
      throw ProblemRest.of(SalesErrorCodes.DUPLICATE_LINE_NUMBER);
    }
    for (var line : command.lines()) {
      validateLine(line);
    }
  }

  public void validateTenantConfiguration(SellTicketCommand command, TenantId tenantId) {
    var gamesByCode = new HashMap<String, TenantGameRefView>();
    tenantGameApi.listGames(tenantId).forEach(game -> gamesByCode.put(game.gameCode(), game));
    var gamesAvailableOnChannel = new HashSet<TenantGameId>();
    drawChannelCatalog
        .listGamesByChannel(tenantId, command.drawChannelId())
        .forEach(
            game -> {
              if (game.enabled()) {
                gamesAvailableOnChannel.add(game.tenantGameId());
              }
            });

    var configsByGame = new HashMap<String, TenantGameBetOptionConfigView>();
    for (var line : command.lines()) {
      var gameCode = line.gameCode().name();
      var tenantGame = gamesByCode.get(gameCode);
      if (tenantGame == null) {
        throw ProblemRest.of(SalesErrorCodes.TENANT_GAME_NOT_CONFIGURED);
      }
      if (!tenantGame.enabled()) {
        throw ProblemRest.of(SalesErrorCodes.TENANT_GAME_DISABLED);
      }
      if (!tenantGame.visibleInPos()) {
        throw ProblemRest.of(SalesErrorCodes.TENANT_GAME_NOT_VISIBLE_IN_POS);
      }
      if (!gamesAvailableOnChannel.contains(tenantGame.tenantGameId())) {
        throw ProblemRest.of(SalesErrorCodes.GAME_NOT_AVAILABLE_ON_DRAW_CHANNEL);
      }
      validateTenantStake(line.stakeAmount(), tenantGame);

      var optionConfig =
          configsByGame.computeIfAbsent(
              gameCode, code -> tenantGameApi.getBetOptionConfig(tenantId, code));
      var betTypeConfig = requireTenantBetTypeConfig(optionConfig, line.betType());
      validateTenantBetOption(line.betOption(), line.betType(), betTypeConfig);
      validateSelection(
          line.betType(),
          effectiveSelectionBetOption(line.betOption(), line.betType(), betTypeConfig),
          line.rawSelection());
    }
  }

  private void validateLine(SellTicketLineInput line) {
    if (line.lineNumber() <= 0) throw ProblemRest.of(SalesErrorCodes.INVALID_LINE_NUMBER);
    if (line.gameCode() == null) throw ProblemRest.of(SalesErrorCodes.GAME_REQUIRED);
    if (line.betType() == null) throw ProblemRest.of(SalesErrorCodes.BET_TYPE_REQUIRED);
    if (!line.gameCode().supports(line.betType()))
      throw ProblemRest.of(SalesErrorCodes.UNSUPPORTED_BET_TYPE);
    if (line.rawSelection() == null || line.rawSelection().isBlank())
      throw ProblemRest.of(SalesErrorCodes.SELECTION_REQUIRED);
    if (line.stakeAmount() == null || line.stakeAmount().signum() <= 0)
      throw ProblemRest.of(SalesErrorCodes.INVALID_STAKE_AMOUNT);
    validateBetOption(line);
    if (!line.betType().requiresOption() || line.betOption() != null) {
      validateSelection(line.betType(), line.betOption(), line.rawSelection());
    }
  }

  private void validateSelection(BetType betType, Short betOption, String rawSelection) {
    try {
      selectionApi.canonicalize(betType, betOption, rawSelection);
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.badRequest(SalesErrorCodes.SELECTION_INVALID, ex);
    }
  }

  private static Short effectiveSelectionBetOption(
      Short betOption, BetType betType, TenantBetTypeOptionConfigView betTypeConfig) {
    if (!betType.requiresOption() || betOption != null) {
      return betOption;
    }
    if (betTypeConfig.selectionPolicy() != SelectionPolicy.IMPLICIT_BEST_MATCH) {
      return betOption;
    }
    return betTypeConfig.options().stream()
        .filter(TenantBetOptionView::enabled)
        .sorted(java.util.Comparator.comparingInt(TenantBetOptionView::displayOrder))
        .map(TenantBetOptionView::code)
        .findFirst()
        .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TENANT_BET_OPTION_NOT_CONFIGURED));
  }

  private void validateBetOption(SellTicketLineInput line) {
    if (line.betOption() == null) {
      return;
    }
    try {
      BetOption.from(line.betType(), line.betOption());
    } catch (IllegalArgumentException ex) {
      if (!line.betType().requiresOption()) {
        throw ProblemRest.badRequest(SalesErrorCodes.BET_OPTION_NOT_ALLOWED, ex);
      }
      throw ProblemRest.badRequest(SalesErrorCodes.BET_OPTION_OUT_OF_RANGE, ex);
    }
  }

  private static void validateTenantStake(BigDecimal stakeAmount, TenantGameRefView tenantGame) {
    if (tenantGame.minStake() != null && stakeAmount.compareTo(tenantGame.minStake()) < 0) {
      throw ProblemRest.of(SalesErrorCodes.STAKE_BELOW_TENANT_MIN);
    }
    if (tenantGame.maxStake() != null && stakeAmount.compareTo(tenantGame.maxStake()) > 0) {
      throw ProblemRest.of(SalesErrorCodes.STAKE_ABOVE_TENANT_MAX);
    }
  }

  private static TenantBetTypeOptionConfigView requireTenantBetTypeConfig(
      TenantGameBetOptionConfigView optionConfig, BetType betType) {
    return optionConfig.betTypes().stream()
        .filter(config -> config.betType() == betType)
        .findFirst()
        .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TENANT_BET_TYPE_NOT_CONFIGURED));
  }

  private static void validateTenantBetOption(
      Short betOption, BetType betType, TenantBetTypeOptionConfigView betTypeConfig) {
    if (!betType.requiresOption()) {
      return;
    }
    if (betTypeConfig.selectionPolicy() == SelectionPolicy.IMPLICIT_BEST_MATCH) {
      if (betOption != null) {
        throw ProblemRest.of(SalesErrorCodes.BET_OPTION_NOT_ALLOWED);
      }
      return;
    }
    if (betOption == null) {
      throw ProblemRest.of(SalesErrorCodes.BET_OPTION_REQUIRED);
    }

    var option = requireTenantBetOption(betOption, betTypeConfig);
    if (!option.enabled()) {
      throw ProblemRest.of(SalesErrorCodes.TENANT_BET_OPTION_DISABLED);
    }
    if (!option.visibleInPos()) {
      throw ProblemRest.of(SalesErrorCodes.TENANT_BET_OPTION_NOT_VISIBLE_IN_POS);
    }
  }

  private static TenantBetOptionView requireTenantBetOption(
      Short betOption, TenantBetTypeOptionConfigView betTypeConfig) {
    return betTypeConfig.options().stream()
        .filter(option -> Objects.equals(option.code(), betOption))
        .findFirst()
        .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TENANT_BET_OPTION_NOT_CONFIGURED));
  }
}

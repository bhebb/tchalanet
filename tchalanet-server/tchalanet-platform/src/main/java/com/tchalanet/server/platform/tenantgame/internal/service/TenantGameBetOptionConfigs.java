package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.model.BetOption;
import com.tchalanet.server.catalog.game.api.model.BetType;
import com.tchalanet.server.catalog.game.api.model.GameCode;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.tenantgame.api.error.TenantGameErrorCodes;
import com.tchalanet.server.platform.tenantgame.api.model.SelectionPolicy;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetOptionConfig;
import com.tchalanet.server.platform.tenantgame.api.model.TenantBetTypeOptionConfig;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetOptionView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantBetTypeOptionConfigView;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameBetOptionConfigView;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class TenantGameBetOptionConfigs {

  List<TenantBetTypeOptionConfig> defaultsFor(String gameCode) {
    var code = GameCode.valueOf(gameCode.toUpperCase());
    return code.allowedBetTypes().stream()
        .sorted(Comparator.comparing(Enum::name))
        .map(this::defaultFor)
        .toList();
  }

  List<TenantBetTypeOptionConfig> effectiveFor(TenantGame tenantGame) {
    var configured = tenantGame.betOptionConfig();
    if (configured == null || configured.isEmpty()) {
      return defaultsFor(tenantGame.gameCode());
    }
    return configured;
  }

  List<TenantBetTypeOptionConfig> normalize(
      String gameCode, List<TenantBetTypeOptionConfig> requested) {
    var defaults = defaultsFor(gameCode);
    if (requested == null || requested.isEmpty()) {
      return defaults;
    }

    validateSupportedBetTypes(gameCode, requested);
    return requested.stream()
        .sorted(Comparator.comparing(config -> config.betType().name()))
        .map(this::normalizeBetType)
        .toList();
  }

  TenantGameBetOptionConfigView toView(TenantGame tenantGame) {
    return new TenantGameBetOptionConfigView(
        tenantGame.gameCode(), effectiveFor(tenantGame).stream().map(this::toView).toList());
  }

  TenantBetTypeOptionConfigView toView(TenantBetTypeOptionConfig config) {
    return new TenantBetTypeOptionConfigView(
        config.betType(),
        config.selectionPolicy(),
        config.defaultOption(),
        config.options().stream()
            .sorted(Comparator.comparingInt(TenantBetOptionConfig::displayOrder))
            .map(option -> toView(config.betType(), option))
            .toList());
  }

  private TenantBetTypeOptionConfig defaultFor(BetType betType) {
    var options =
        BetOption.allowedFor(betType).stream().filter(BetOption::enabledByDefault).toList();
    return new TenantBetTypeOptionConfig(
        betType,
        defaultSelectionPolicy(betType),
        options.isEmpty() ? null : options.getFirst().code(),
        options.stream()
            .map(option -> new TenantBetOptionConfig(option.code(), true, true, option.code()))
            .toList());
  }

  private SelectionPolicy defaultSelectionPolicy(BetType betType) {
    return switch (betType) {
      case MARRIAGE_2D2D, LOTTO3_3D, LOTTO4_PATTERN, LOTTO5_PATTERN ->
          SelectionPolicy.IMPLICIT_BEST_MATCH;
      case MATCH_1_2D, MATCH_2_2D, MATCH_3_2D -> SelectionPolicy.EXPLICIT_ONLY;
    };
  }

  private TenantBetTypeOptionConfig normalizeBetType(TenantBetTypeOptionConfig config) {
    if (config.betType() == null) {
      throw ProblemRest.of(TenantGameErrorCodes.BET_TYPE_REQUIRED);
    }
    var policy =
        config.selectionPolicy() == null ? SelectionPolicy.EXPLICIT_ONLY : config.selectionPolicy();

    var allowedOptions = BetOption.allowedFor(config.betType());
    if (allowedOptions.isEmpty()) {
      if (config.defaultOption() != null) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_OPTION_DEFAULT_INVALID);
      }
      return new TenantBetTypeOptionConfig(config.betType(), policy, null, List.of());
    }

    var requestedOptions =
        config.options() == null || config.options().isEmpty()
            ? defaultFor(config.betType()).options()
            : config.options();
    validateOptions(config.betType(), requestedOptions, config.defaultOption());
    var normalizedOptions =
        requestedOptions.stream()
            .sorted(Comparator.comparingInt(TenantBetOptionConfig::displayOrder))
            .toList();
    var defaultOption =
        config.defaultOption() != null
            ? config.defaultOption()
            : normalizedOptions.stream()
                .filter(TenantBetOptionConfig::enabled)
                .findFirst()
                .map(TenantBetOptionConfig::code)
                .orElseThrow(
                    () -> ProblemRest.of(TenantGameErrorCodes.BET_OPTION_ENABLED_REQUIRED));
    return new TenantBetTypeOptionConfig(
        config.betType(), policy, defaultOption, normalizedOptions);
  }

  private void validateSupportedBetTypes(String gameCode, List<TenantBetTypeOptionConfig> configs) {
    var code = GameCode.valueOf(gameCode.toUpperCase());
    var seen = new HashSet<BetType>();
    for (var config : configs) {
      if (config.betType() == null) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_TYPE_REQUIRED);
      }
      if (!code.supports(config.betType())) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_TYPE_UNSUPPORTED);
      }
      if (!seen.add(config.betType())) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_TYPE_DUPLICATE);
      }
    }
  }

  private void validateOptions(
      BetType betType, List<TenantBetOptionConfig> options, Short defaultOption) {
    var seen = new HashSet<Short>();
    var enabled = new HashSet<Short>();
    for (var option : options) {
      try {
        BetOption.from(betType, option.code());
      } catch (IllegalArgumentException ex) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_OPTION_INVALID, java.util.Map.of(), ex);
      }
      if (!seen.add(option.code())) {
        throw ProblemRest.of(TenantGameErrorCodes.BET_OPTION_DUPLICATE);
      }
      if (option.displayOrder() < 0) {
        throw ProblemRest.of(TenantGameErrorCodes.DISPLAY_ORDER_INVALID);
      }
      if (option.enabled()) {
        enabled.add(option.code());
      }
    }
    if (enabled.isEmpty()) {
      throw ProblemRest.of(TenantGameErrorCodes.BET_OPTION_ENABLED_REQUIRED);
    }
    if (defaultOption != null && !enabled.contains(defaultOption)) {
      throw ProblemRest.of(TenantGameErrorCodes.BET_OPTION_DEFAULT_INVALID);
    }
  }

  private TenantBetOptionView toView(BetType betType, TenantBetOptionConfig config) {
    var option = BetOption.from(betType, config.code());
    return new TenantBetOptionView(
        config.code(),
        option.label(),
        option.description(),
        config.enabled(),
        config.visibleInPos(),
        config.displayOrder());
  }
}

package com.tchalanet.server.catalog.resultslot.internal.write;

import com.tchalanet.server.catalog.resultslot.api.error.ResultSlotErrorCodes;
import com.tchalanet.server.common.web.error.ProblemRest;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
class ResultSlotConfigValidator {

  private static final Set<String> SOURCE_GAME_KEYS = Set.of("pick3", "pick4");
  private static final Set<String> HAITI_LOT_KEYS = Set.of("lot1", "lot2", "lot3", "lot4");
  private static final Set<String> HAITI_PROJECTION_TOKENS =
      Set.of(
          "PICK3_FULL_3",
          "PICK3_FIRST2",
          "PICK3_LAST2",
          "PICK4_FULL_4",
          "PICK4_FIRST2",
          "PICK4_LAST2");

  void validateSourceCfg(JsonNode sourceCfg) {
    requireObject(sourceCfg, "source_cfg");
    requireText(sourceCfg.get("provider_slot_code"), "source_cfg.provider_slot_code");

    var hasActiveGame = false;
    for (var gameKey : SOURCE_GAME_KEYS) {
      var game = sourceCfg.get(gameKey);
      if (game == null || game.isNull()) {
        continue;
      }
      requireObject(game, "source_cfg." + gameKey);
      requireText(game.get("game_code"), "source_cfg." + gameKey + ".game_code");
      var active = game.get("active");
      if (active == null || active.isNull() || !active.isBoolean()) {
        throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
      }
      hasActiveGame = hasActiveGame || active.asBoolean(false);
    }

    if (!hasActiveGame) {
      throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
    }
  }

  void validateProjectionCfg(JsonNode projectionCfg) {
    requireObject(projectionCfg, "projection_cfg");
    var rules = projectionCfg.get("rules");
    requireObject(rules, "projection_cfg.rules");

    for (var entry : rules.properties()) {
      var lotKey = normalizeKey(entry.getKey());
      if (!HAITI_LOT_KEYS.contains(lotKey)) {
        throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
      }
      var token =
          requireText(entry.getValue(), "projection_cfg.rules." + lotKey).toUpperCase(Locale.ROOT);
      if (!HAITI_PROJECTION_TOKENS.contains(token)) {
        throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
      }
    }

    for (var lotKey : HAITI_LOT_KEYS) {
      if (rules.get(lotKey) == null || rules.get(lotKey).isNull()) {
        throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
      }
    }
  }

  private static void requireObject(JsonNode node, String field) {
    if (node == null || node.isNull() || !node.isObject()) {
      throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
    }
  }

  private static String requireText(JsonNode node, String field) {
    if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
      throw ProblemRest.of(ResultSlotErrorCodes.CONFIG_INVALID);
    }
    return node.asText().trim();
  }

  private static String normalizeKey(String key) {
    return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
  }
}

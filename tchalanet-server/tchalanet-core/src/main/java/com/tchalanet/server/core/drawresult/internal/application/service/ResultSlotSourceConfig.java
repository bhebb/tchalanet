package com.tchalanet.server.core.drawresult.internal.application.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;

public record ResultSlotSourceConfig(
    String providerSlotCode,
    SourceGame pick3,
    SourceGame pick4
) {

    public static ResultSlotSourceConfig empty() {
        return new ResultSlotSourceConfig("", null, null);
    }

    public boolean hasProviderSlotCode() {
        return providerSlotCode != null && !providerSlotCode.isBlank();
    }

    public boolean hasAnyActiveGame() {
        return isActive(pick3) || isActive(pick4);
    }

    public Optional<SourceGame> activePick3() {
        return isActive(pick3) ? Optional.of(pick3) : Optional.empty();
    }

    public Optional<SourceGame> activePick4() {
        return isActive(pick4) ? Optional.of(pick4) : Optional.empty();
    }

    public Set<String> activeGameCodes() {
        return Stream.of(pick3, pick4)
            .filter(ResultSlotSourceConfig::isActive)
            .map(SourceGame::gameCode)
            .collect(Collectors.toUnmodifiableSet());
    }

    public static boolean isActive(SourceGame game) {
        return game != null
            && game.active()
            && game.gameCode() != null
            && !game.gameCode().isBlank();
    }

    static String providerSlotCodeFrom(JsonNode sourceCfg) {
        if (sourceCfg == null || sourceCfg.isNull() || !sourceCfg.isObject()) {
            return "";
        }

        var node = sourceCfg.get("provider_slot_code");

        if (node == null || node.isNull()) {
            return "";
        }

        return normalize(node.asText(""));
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
    }

    public record SourceGame(String gameCode, boolean active) {

        static SourceGame from(JsonNode node) {
            if (node == null || node.isNull() || !node.isObject()) {
                return null;
            }

            var codeNode = node.get("game_code");
            var activeNode = node.get("active");

            var code =
                codeNode == null || codeNode.isNull()
                    ? ""
                    : normalizeGameCode(codeNode.asText(""));

            var active = activeNode == null || activeNode.isNull() || activeNode.asBoolean(true);

            return new SourceGame(code, active);
        }

        private static String normalizeGameCode(String value) {
            return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        }
    }
}

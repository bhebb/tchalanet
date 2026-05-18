package com.tchalanet.server.core.uslottery.internal.infra.external;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Matches Tchalanet result_slot.source_cfg.provider_slot_code against provider-native
 * slot labels/draw types.
 *
 * Internal slot_key remains the Tchalanet identity (GA_EVE, TX_1000, ...).
 * provider_slot_code is the external-provider matching hint (MIDDAY, EVENING, ...).
 */
public final class ProviderSlotCodeMatcher {

    private static final Map<String, Set<String>> ALIASES =
        Map.of(
            "MIDDAY", Set.of("MIDDAY", "MID", "MIDD", "MIDDRAW", "MIDDAYDRAW", "MIDDAYDRAWING"),
            "EVENING", Set.of("EVENING", "EVE", "EVENINGDRAW", "EVENINGDRAWING"),
            "MORNING", Set.of("MORNING", "MORN", "MORNINGDRAW", "MORNINGDRAWING"),
            "DAY", Set.of("DAY", "DAYTIME", "DAYDRAW", "DAYDRAWING"),
            "NIGHT", Set.of("NIGHT", "LATE", "LATENIGHT", "LATENIGHTDRAW", "LATENIGHTDRAWING"));

    private ProviderSlotCodeMatcher() {}

    public static boolean matches(String actual, String expected) {
        var expectedNorm = normalize(expected);
        if (expectedNorm.isBlank()) {
            return true; // Backward-compatible while migrations are rolling out.
        }

        var actualNorm = normalize(actual);
        if (actualNorm.isBlank()) {
            return false;
        }

        if (actualNorm.equals(expectedNorm)) {
            return true;
        }

        return ALIASES.getOrDefault(expectedNorm, Set.of(expectedNorm)).contains(actualNorm);
    }

    public static String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_\\-]+", "");
    }
}

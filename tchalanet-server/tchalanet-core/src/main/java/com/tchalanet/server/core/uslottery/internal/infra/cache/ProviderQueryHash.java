package com.tchalanet.server.core.uslottery.internal.infra.cache;

import com.tchalanet.server.common.util.Hashing;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ProviderQueryHash {

    private ProviderQueryHash() {}

    public static String of(
        String provider,
        LocalDate date,
        LocalTime drawTime,
        List<String> codes,
        String shape
    ) {
        String canonical =
            "v2"
                + "|provider="
                + normProvider(provider)
                + "|date="
                + (date == null ? "" : date)
                + "|time="
                + (drawTime == null ? "" : drawTime)
                + "|codes="
                + String.join(",", normCodes(codes))
                + "|shape="
                + normShape(shape);

        return Hashing.sha256Hex(canonical);
    }

    private static String normProvider(String provider) {
        var trimmed = provider == null ? "" : provider.trim();
        return trimmed.replace("-", "_").toUpperCase(Locale.ROOT);
    }

    private static String normShape(String shape) {
        return shape == null ? "" : shape.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> normCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        final Set<String> normalized = new LinkedHashSet<>();

        for (String code : codes) {
            if (code == null) {
                continue;
            }
            var trimmedCode = code.trim().toUpperCase(Locale.ROOT);
            if (!trimmedCode.isBlank()) {
                normalized.add(trimmedCode);
            }
        }

        final List<String> out = new ArrayList<>(normalized);
        out.sort(Comparator.naturalOrder());
        return List.copyOf(out);
    }
}

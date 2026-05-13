package com.tchalanet.server.core.uslottery.internal.infra.external.ga;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeorgiaDrawResultsMapper {

    private static final String ORIGIN = "GA_API";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query
    ) {
        var entries = parseEntries(body);

        if (entries.isEmpty()) {
            return UsLotteryProviderResponse.empty(UsLotteryProvider.GA, query);
        }

        var wantedCodes = normalizeWantedCodes(query.externalGameCodes());

        var results = entries.stream()
            .map(entry -> mapEntry(entry, wantedCodes, sourceHash, url, query))
            .filter(Objects::nonNull)
            .toList();

        return new UsLotteryProviderResponse(
            UsLotteryProvider.GA,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            results,
            query.includeRaw() ? body : null
        );
    }

    private UsLotteryProviderResult mapEntry(
        GeorgiaDrawEntry entry,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query
    ) {
        if (entry == null || !"CLOSED".equalsIgnoreCase(entry.status())) {
            return null;
        }

        var gameCode = normalizeProviderGameCode(entry.gameName());

        if (gameCode.isBlank()) {
            return null;
        }

        if (!wantedCodes.isEmpty() && !wantedCodes.contains(gameCode)) {
            log.debug("ga-client skipped externalGameCode={} wanted={}", gameCode, wantedCodes);
            return null;
        }

        var drawDate = resolveDrawDate(entry, query);

        if (drawDate != null && query.drawDate() != null && !query.drawDate().equals(drawDate)) {
            return null;
        }

        var firstResult = entry.results() == null || entry.results().isEmpty()
            ? null
            : entry.results().getFirst();

        var main = parseMainDigits(firstResult);

        if (main.isEmpty()) {
            return null;
        }

        var expectedSize = expectedSize(gameCode);
        var quality = expectedSize > 0 && main.size() == expectedSize
            ? ResultQuality.COMPLETE
            : ResultQuality.SUSPECT;

        var providerDrawType = resolveProviderDrawType(entry);

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", UsLotteryProvider.GA.name());
        metadata.put("game_code", gameCode);
        metadata.put("draw_date", drawDate == null ? "" : drawDate.toString());

        if (!providerDrawType.isBlank()) {
            metadata.put("provider_draw_type", providerDrawType);
        }

        var flags = new UsProviderSourceFlags(
            ORIGIN,
            sourceHash,
            url,
            Map.copyOf(metadata)
        );

        return new UsLotteryProviderResult(
            gameCode,
            main,
            List.of(),
            quality,
            flags,
            resolveOccurredAt(entry, query),
            query.includeRaw() ? entry : null
        );
    }

    private List<GeorgiaDrawEntry> parseEntries(String body) {
        try {
            JsonNode root = json.readTree(body);

            if (root != null && root.isArray()) {
                return json.fromJson(body, new TypeReference<List<GeorgiaDrawEntry>>() {});
            }

            JsonNode draws = root == null ? null : root.get("draws");

            if (draws != null && draws.isArray()) {
                return json.convertValue(draws, new TypeReference<List<GeorgiaDrawEntry>>() {});
            }
        } catch (Exception ex) {
            log.warn("ga-client parse failed: {}", ex.getLocalizedMessage(), ex);
        }

        return List.of();
    }

    private static LocalDate resolveDrawDate(GeorgiaDrawEntry entry, UsLotteryProviderQuery query) {
        if (entry.drawTime() != null) {
            try {
                return Instant.ofEpochMilli(entry.drawTime())
                    .atZone(query.timezone())
                    .toLocalDate();
            } catch (Exception ex) {
                log.warn("Failed to resolve GA draw date from epoch: {}", ex.getLocalizedMessage(), ex);
            }
        }

        return parseIsoDate(entry.drawDate());
    }

    private static Instant resolveOccurredAt(GeorgiaDrawEntry entry, UsLotteryProviderQuery query) {
        if (entry.drawTime() != null) {
            try {
                return Instant.ofEpochMilli(entry.drawTime());
            } catch (Exception ex) {
                log.warn("Failed to resolve GA occurredAt from epoch: {}", ex.getLocalizedMessage(), ex);
            }
        }

        return query.drawDate()
            .atTime(query.drawTime())
            .atZone(query.timezone())
            .toInstant();
    }

    private static List<String> parseMainDigits(GeorgiaDrawResult result) {
        if (result == null || result.primary() == null) {
            return List.of();
        }

        return result.primary().stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(digit -> !digit.isBlank())
            .toList();
    }

    /**
     * Keep provider-native codes.
     *
     * GA source_cfg should use:
     * - CASH3 for pick3
     * - CASH4 for pick4
     */
    private static String normalizeProviderGameCode(String raw) {
        return normalize(raw);
    }

    private static int expectedSize(String gameCode) {
        return switch (gameCode) {
            case "CASH3", "PICK3" -> 3;
            case "CASH4", "PICK4" -> 4;
            default -> 0;
        };
    }

    /**
     * Compatibility bridge:
     * - preferred wanted codes: CASH3/CASH4
     * - accepted legacy aliases: PICK3/PICK4
     *
     * The mapper still returns provider-native CASH3/CASH4.
     */
    private static Set<String> normalizeWantedCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
            .filter(Objects::nonNull)
            .map(GeorgiaDrawResultsMapper::normalize)
            .filter(normalizedCode -> !normalizedCode.isBlank())
            .flatMap(code -> aliases(code).stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> aliases(String code) {
        return switch (code) {
            case "PICK3", "CASH3" -> Set.of("CASH3", "PICK3");
            case "PICK4", "CASH4" -> Set.of("CASH4", "PICK4");
            default -> Set.of(code);
        };
    }

    private static String resolveProviderDrawType(GeorgiaDrawEntry entry) {
        var type = normalize(entry.name());

        if (type.isBlank() && entry.results() != null && !entry.results().isEmpty()) {
            type = normalize(entry.results().getFirst().drawType());
        }

        return type;
    }

    private static LocalDate parseIsoDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            var s = raw.trim();
            var first = s.length() >= 10 ? s.substring(0, 10) : s;
            return LocalDate.parse(first);
        } catch (Exception ex) {
            log.warn("Failed to parse GA draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeorgiaDrawEntry(
        @JsonProperty("drawDate") String drawDate,
        @JsonProperty("gameName") String gameName,
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("drawTime") Long drawTime,
        @JsonProperty("results") List<GeorgiaDrawResult> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeorgiaDrawResult(
        @JsonProperty("primary") List<String> primary,
        @JsonProperty("drawType") String drawType
    ) {}
}

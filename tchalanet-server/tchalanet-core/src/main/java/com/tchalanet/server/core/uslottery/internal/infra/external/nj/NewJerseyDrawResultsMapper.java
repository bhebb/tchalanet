package com.tchalanet.server.core.uslottery.internal.infra.external.nj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/**
 * Maps the NJ draw-games feed. Shares the draw-games platform shape used by GA:
 * each entry exposes {@code gameName}, a slot label ({@code name}/{@code drawType}),
 * a {@code status}, a {@code drawTime} epoch and {@code results[].primary} winning digits.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NewJerseyDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NJ;
    private static final String ORIGIN = "NJ_API";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body, String sourceHash, String url, UsLotteryProviderQuery query) {

        var entries = parseEntries(body);
        if (entries.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var wantedCodes = normalizeWantedCodes(query.externalGameCodes());

        var results = new ArrayList<UsLotteryProviderResult>();
        for (var entry : entries) {
            try {
                var mapped = mapEntry(entry, wantedCodes, sourceHash, url, query);
                if (mapped != null) {
                    results.add(mapped);
                }
            } catch (Exception ex) {
                log.warn(
                    "nj-client skipped invalid entry drawDate={} providerSlotCode={} err={}",
                    query.drawDate(), query.providerSlotCode(), ex.getMessage(), ex);
            }
        }

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(results),
            query.includeRaw() ? body : null);
    }

    private UsLotteryProviderResult mapEntry(
        NjDrawEntry entry,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        if (entry == null || !"CLOSED".equalsIgnoreCase(entry.status())) {
            return null;
        }

        var gameCode = normalizeGameCode(entry.gameName());
        if (gameCode.isBlank()) {
            return null;
        }
        if (!wantedCodes.isEmpty() && !wantedCodes.contains(gameCode)) {
            return null;
        }

        var providerDrawType = resolveProviderDrawType(entry);
        if (!ProviderSlotCodeMatcher.matches(providerDrawType, query.providerSlotCode())) {
            return null;
        }

        var drawDate = resolveDrawDate(entry, query);
        if (drawDate != null && !query.drawDate().equals(drawDate)) {
            return null;
        }

        // Each draw carries a FIREBALL variant and the Regular draw; only the latter holds the
        // official winning number, as a single concatenated string (e.g. "246").
        var regular = entry.results() == null ? null : entry.results().stream()
            .filter(r -> r != null && "Regular".equalsIgnoreCase(r.drawType()))
            .findFirst()
            .orElse(null);
        var main = parseMainDigits(regular);
        if (main.isEmpty()) {
            return null;
        }

        var expectedSize = expectedSize(gameCode);
        var quality = expectedSize > 0 && main.size() == expectedSize
            ? ResultQuality.COMPLETE
            : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", gameCode);
        metadata.put("draw_date", drawDate == null ? "" : drawDate.toString());
        metadata.put("provider_slot_code", providerDrawType);
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));

        return new UsLotteryProviderResult(
            gameCode,
            main,
            List.of(),
            quality,
            flags,
            query.drawDate().atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? entry : null);
    }

    private List<NjDrawEntry> parseEntries(String body) {
        try {
            JsonNode root = json.readTree(body);
            if (root == null) {
                return List.of();
            }
            if (root.isArray()) {
                return json.fromJson(body, new TypeReference<List<NjDrawEntry>>() {});
            }
            // Paginated payloads wrap the rows under "draws" or "content".
            for (var field : List.of("draws", "content")) {
                var node = root.get(field);
                if (node != null && node.isArray()) {
                    return json.convertValue(node, new TypeReference<List<NjDrawEntry>>() {});
                }
            }
        } catch (Exception ex) {
            log.warn("nj-client parse failed: {}", ex.getMessage(), ex);
        }
        return List.of();
    }

    private static LocalDate resolveDrawDate(NjDrawEntry entry, UsLotteryProviderQuery query) {
        if (entry.drawTime() != null) {
            try {
                return Instant.ofEpochMilli(entry.drawTime()).atZone(query.timezone()).toLocalDate();
            } catch (Exception ignored) {
                // fall through to ISO field
            }
        }
        return parseIsoDate(entry.drawDate());
    }

    /**
     * The Regular result carries the winning number as a single concatenated string (e.g. "246");
     * split it into individual digits.
     */
    private static List<String> parseMainDigits(NjDrawResult result) {
        if (result == null || result.primary() == null || result.primary().isEmpty()) {
            return List.of();
        }
        var raw = result.primary().getFirst();
        if (raw == null) {
            return List.of();
        }
        var out = new ArrayList<String>();
        for (var c : raw.trim().toCharArray()) {
            if (Character.isDigit(c)) {
                out.add(String.valueOf(c));
            }
        }
        return out;
    }

    private static String resolveProviderDrawType(NjDrawEntry entry) {
        // The slot lives on the draw's name (MIDDAY / EVENING); the per-result drawType is the game
        // variant (Regular / FIREBALL), not a slot, so it is not used as a fallback here.
        return ProviderSlotCodeMatcher.normalize(entry.name());
    }

    private static String normalizeGameCode(String raw) {
        var n = ProviderSlotCodeMatcher.normalize(raw);
        return switch (n) {
            case "PICK3", "CASH3" -> "PICK3";
            case "PICK4", "CASH4" -> "PICK4";
            default -> n.startsWith("PICK3") ? "PICK3" : n.startsWith("PICK4") ? "PICK4" : "";
        };
    }

    private static int expectedSize(String gameCode) {
        return switch (gameCode) {
            case "PICK3" -> 3;
            case "PICK4" -> 4;
            default -> 0;
        };
    }

    private static Set<String> normalizeWantedCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return codes.stream()
            .filter(Objects::nonNull)
            .map(NewJerseyDrawResultsMapper::normalizeGameCode)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }

    private static LocalDate parseIsoDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            var s = raw.trim();
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception ex) {
            log.warn("nj-client failed to parse date '{}': {}", raw, ex.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NjDrawEntry(
        @JsonProperty("gameName") String gameName,
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("drawTime") Long drawTime,
        @JsonProperty("drawDate") String drawDate,
        @JsonProperty("results") List<NjDrawResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NjDrawResult(
        @JsonProperty("primary") List<String> primary,
        @JsonProperty("drawType") String drawType) {}
}

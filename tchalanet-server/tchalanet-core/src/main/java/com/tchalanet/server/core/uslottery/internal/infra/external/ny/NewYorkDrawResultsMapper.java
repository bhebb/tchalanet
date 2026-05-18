package com.tchalanet.server.core.uslottery.internal.infra.external.ny;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewYorkDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;
    private static final String ORIGIN = "NY_SOCRATA";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body,
        String sourceHash,
        UsLotteryProviderQuery query) {

        var entries = parseEntries(body);
        if (entries.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var wantedCodes = normalizeSet(query.externalGameCodes());
        var slot = resolveSlot(query.providerSlotCode());

        if (slot == Slot.UNKNOWN) {
            log.warn("ny-client unsupported providerSlotCode={}", query.providerSlotCode());
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var results = new ArrayList<UsLotteryProviderResult>();

        for (var entry : entries) {
            var drawDate = parseDate(entry.drawDate());
            if (drawDate == null || !query.drawDate().equals(drawDate)) {
                continue;
            }

            addResult(results, "NUMBERS", slot.pick3(entry), 3, wantedCodes, sourceHash, query, entry, drawDate, slot);
            addResult(results, "WIN4", slot.pick4(entry), 4, wantedCodes, sourceHash, query, entry, drawDate, slot);

            // NY endpoint is ordered DESC; once target date is found, no need to inspect older rows.
            break;
        }

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(results),
            query.includeRaw() ? body : null);
    }

    private void addResult(
        List<UsLotteryProviderResult> out,
        String gameCode,
        String rawDigits,
        int expectedSize,
        Set<String> wantedCodes,
        String sourceHash,
        UsLotteryProviderQuery query,
        NewYorkEntry entry,
        LocalDate drawDate,
        Slot slot) {

        if (!wantedCodes.isEmpty() && !wantedCodes.contains(gameCode)) {
            return;
        }

        var main = parseDigits(rawDigits);
        if (main.isEmpty()) {
            return;
        }

        var quality = main.size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", gameCode);
        metadata.put("draw_date", String.valueOf(drawDate));
        metadata.put("provider_slot_code", slot.providerSlotCode);
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                sourceHash,
                "",
                Map.copyOf(metadata));

        out.add(
            new UsLotteryProviderResult(
                gameCode,
                main,
                List.of(),
                quality,
                flags,
                resolveOccurredAt(query),
                query.includeRaw() ? entry : null));
    }

    private List<NewYorkEntry> parseEntries(String body) {
        try {
            JsonNode root = json.readTree(body);

            if (root != null && root.isArray()) {
                return json.fromJson(body, new TypeReference<>() {});
            }

            JsonNode data = root == null ? null : root.get("data");
            if (data != null && data.isArray()) {
                return json.convertValue(data, new TypeReference<>() {});
            }
        } catch (Exception ex) {
            log.warn("ny-client parse failed: {}", ex.getLocalizedMessage(), ex);
        }

        return List.of();
    }

    private static Slot resolveSlot(String providerSlotCode) {
        var code = ProviderSlotCodeMatcher.normalize(providerSlotCode);

        if (ProviderSlotCodeMatcher.matches("MIDDAY", code)) {
            return Slot.MIDDAY;
        }

        if (ProviderSlotCodeMatcher.matches("EVENING", code)) {
            return Slot.EVENING;
        }

        return Slot.UNKNOWN;
    }

    private static Instant resolveOccurredAt(UsLotteryProviderQuery query) {
        return query.drawDate()
            .atTime(query.drawTime())
            .atZone(query.timezone())
            .toInstant();
    }

    private static List<String> parseDigits(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        var compact = raw.trim().replaceAll("[^0-9]", "");

        if (compact.isBlank()) {
            return List.of();
        }

        return compact.chars()
            .mapToObj(ch -> String.valueOf((char) ch))
            .toList();
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            var s = raw.trim();
            var first = s.length() >= 10 ? s.substring(0, 10) : s;
            return LocalDate.parse(first);
        } catch (Exception ex) {
            log.warn("Failed to parse NY draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static Set<String> normalizeSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
            .filter(Objects::nonNull)
            .map(NewYorkDrawResultsMapper::normalizeGameCode)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalizeGameCode(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private enum Slot {
        MIDDAY("MIDDAY") {
            @Override String pick3(NewYorkEntry e) { return e.middayDaily(); }
            @Override String pick4(NewYorkEntry e) { return e.middayWin4(); }
        },
        EVENING("EVENING") {
            @Override String pick3(NewYorkEntry e) { return e.eveningDaily(); }
            @Override String pick4(NewYorkEntry e) { return e.eveningWin4(); }
        },
        UNKNOWN("") {
            @Override String pick3(NewYorkEntry e) { return ""; }
            @Override String pick4(NewYorkEntry e) { return ""; }
        };

        private final String providerSlotCode;

        Slot(String providerSlotCode) {
            this.providerSlotCode = providerSlotCode;
        }

        abstract String pick3(NewYorkEntry e);
        abstract String pick4(NewYorkEntry e);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewYorkEntry(
        @JsonProperty("draw_date") String drawDate,
        @JsonProperty("midday_daily") String middayDaily,
        @JsonProperty("evening_daily") String eveningDaily,
        @JsonProperty("midday_win_4") String middayWin4,
        @JsonProperty("evening_win_4") String eveningWin4) {}
}

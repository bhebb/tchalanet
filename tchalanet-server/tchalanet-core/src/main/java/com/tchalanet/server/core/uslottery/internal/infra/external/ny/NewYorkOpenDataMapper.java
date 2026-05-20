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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewYorkOpenDataMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;
    private static final String ORIGIN = "NY_OPEN_DATA";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query,
        String sourceStrategy
    ) {
        var entries = parseEntries(body);
        if (entries.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var slot = resolveSlot(query.providerSlotCode());
        if (slot == Slot.UNKNOWN) {
            log.warn("ny-open-data unsupported providerSlotCode={}", query.providerSlotCode());
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var results = new ArrayList<UsLotteryProviderResult>();
        var latestDates = new ArrayList<String>();

        for (var entry : entries) {
            latestDates.add(entry.drawDate());

            var drawDate = parseDate(entry.drawDate());
            if (drawDate == null || !query.drawDate().equals(drawDate)) {
                continue;
            }

            addResult(results, "NUMBERS", slot.pick3(entry), 3, sourceHash, url, query, entry, drawDate, slot, sourceStrategy);
            addResult(results, "WIN4", slot.pick4(entry), 4, sourceHash, url, query, entry, drawDate, slot, sourceStrategy);
            break;
        }

        if (results.isEmpty()) {
            log.warn(
                "ny-open-data no matching rows requestedDate={} providerSlotCode={} latestProviderDates={}",
                query.drawDate(),
                query.providerSlotCode(),
                latestDates.stream().limit(5).toList());
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
        String sourceHash,
        String url,
        UsLotteryProviderQuery query,
        NewYorkOpenDataEntry entry,
        LocalDate drawDate,
        Slot slot,
        String sourceStrategy) {

        if (!query.externalGameCodes().contains(gameCode)) {
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
        metadata.put("source_strategy", sourceStrategy);

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                sourceHash,
                url,
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

    private List<NewYorkOpenDataEntry> parseEntries(String body) {
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
            log.warn("ny-open-data parse failed: {}", ex.getLocalizedMessage(), ex);
        }

        return List.of();
    }

    private static Slot resolveSlot(String providerSlotCode) {
        if (ProviderSlotCodeMatcher.matches("MIDDAY", providerSlotCode)) {
            return Slot.MIDDAY;
        }

        if (ProviderSlotCodeMatcher.matches("EVENING", providerSlotCode)) {
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
            var first = raw.trim().length() >= 10 ? raw.trim().substring(0, 10) : raw.trim();
            return LocalDate.parse(first);
        } catch (Exception ex) {
            log.warn("Failed to parse NY Open Data draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private enum Slot {
        MIDDAY("MIDDAY") {
            @Override String pick3(NewYorkOpenDataEntry e) { return e.middayDaily(); }
            @Override String pick4(NewYorkOpenDataEntry e) { return e.middayWin4(); }
        },
        EVENING("EVENING") {
            @Override String pick3(NewYorkOpenDataEntry e) { return e.eveningDaily(); }
            @Override String pick4(NewYorkOpenDataEntry e) { return e.eveningWin4(); }
        },
        UNKNOWN("") {
            @Override String pick3(NewYorkOpenDataEntry e) { return ""; }
            @Override String pick4(NewYorkOpenDataEntry e) { return ""; }
        };

        private final String providerSlotCode;

        Slot(String providerSlotCode) {
            this.providerSlotCode = providerSlotCode;
        }

        abstract String pick3(NewYorkOpenDataEntry e);
        abstract String pick4(NewYorkOpenDataEntry e);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewYorkOpenDataEntry(
        @JsonProperty("draw_date") String drawDate,
        @JsonProperty("midday_daily") String middayDaily,
        @JsonProperty("evening_daily") String eveningDaily,
        @JsonProperty("midday_win_4") String middayWin4,
        @JsonProperty("evening_win_4") String eveningWin4) {}
}

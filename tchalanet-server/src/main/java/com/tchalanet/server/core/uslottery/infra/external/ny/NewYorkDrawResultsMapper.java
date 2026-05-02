package com.tchalanet.server.core.uslottery.infra.external.ny;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.application.port.out.UsProviderSourceFlags;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewYorkDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.NY;
    private static final String ORIGIN = "NY_OPEN_DATA";

    private final JsonbUtils jsonb;

    public UsLotteryProviderResponse map(
        String body,
        String sourceHash,
        UsLotteryProviderQuery query) {

        var rows = parseRows(body);
        if (rows.length == 0) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var wantedCodes = normalizeSet(query.gameCodes());
        var wantedSlot = resolveWantedSlot(query.drawTime());

        var results = new java.util.ArrayList<UsLotteryProviderResult>();

        for (var row : rows) {
            var drawDate = parseDrawDate(row.drawDate());
            if (!query.drawDate().equals(drawDate)) {
                continue;
            }

            if (wantedSlot == Slot.MIDDAY || wantedSlot == Slot.UNKNOWN) {
                addResultIfWanted(
                    results,
                    "NUMBERS",
                    row.middayDaily(),
                    Slot.MIDDAY,
                    drawDate,
                    wantedCodes,
                    sourceHash,
                    query);

                addResultIfWanted(
                    results,
                    "WIN4",
                    row.middayWin4(),
                    Slot.MIDDAY,
                    drawDate,
                    wantedCodes,
                    sourceHash,
                    query);
            }

            if (wantedSlot == Slot.EVENING || wantedSlot == Slot.UNKNOWN) {
                addResultIfWanted(
                    results,
                    "NUMBERS",
                    row.eveningDaily(),
                    Slot.EVENING,
                    drawDate,
                    wantedCodes,
                    sourceHash,
                    query);

                addResultIfWanted(
                    results,
                    "WIN4",
                    row.eveningWin4(),
                    Slot.EVENING,
                    drawDate,
                    wantedCodes,
                    sourceHash,
                    query);
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

    private NyRow[] parseRows(String body) {
        try {
            var rows = jsonb.fromJson(body, NyRow[].class);
            return rows == null ? new NyRow[0] : rows;
        } catch (Exception ex) {
            log.warn("Failed to parse NY lottery response: {}", ex.getLocalizedMessage(), ex);
            return new NyRow[0];
        }
    }

    private void addResultIfWanted(
        List<UsLotteryProviderResult> out,
        String gameCode,
        String rawDigits,
        Slot slot,
        LocalDate drawDate,
        Set<String> wantedCodes,
        String sourceHash,
        UsLotteryProviderQuery query) {

        var normalizedGameCode = normalize(gameCode);
        if (!wantedCodes.isEmpty() && !wantedCodes.contains(normalizedGameCode)) {
            return;
        }

        var main = digits(rawDigits);
        if (main.isEmpty()) {
            return;
        }

        var expectedSize = expectedSize(normalizedGameCode);
        var quality = main.size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", normalizedGameCode);
        metadata.put("draw_date", String.valueOf(drawDate));
        metadata.put("provider_slot", slot.name());

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                sourceHash,
                null,
                Map.copyOf(metadata));

        out.add(
            new UsLotteryProviderResult(
                normalizedGameCode,
                main,
                List.of(),
                quality,
                flags,
                occurredAt(drawDate, slot, query),
                null));
    }

    private static Instant occurredAt(LocalDate drawDate, Slot slot, UsLotteryProviderQuery query) {
        var time =
            switch (slot) {
                case MIDDAY -> LocalTime.of(12, 20);
                case EVENING -> LocalTime.of(19, 30);
                case UNKNOWN -> query.drawTime();
            };

        return drawDate.atTime(time).atZone(query.timezone()).toInstant();
    }

    private static Slot resolveWantedSlot(LocalTime drawTime) {
        if (drawTime == null) {
            return Slot.UNKNOWN;
        }

        if (drawTime.isBefore(LocalTime.of(16, 0))) {
            return Slot.MIDDAY;
        }

        return Slot.EVENING;
    }

    private static LocalDate parseDrawDate(String raw) {
        if (raw == null || raw.isBlank() || raw.length() < 10) {
            return null;
        }

        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (Exception ex) {
            log.warn("Failed to parse NY draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static int expectedSize(String gameCode) {
        return switch (gameCode) {
            case "NUMBERS" -> 3;
            case "WIN4" -> 4;
            default -> 0;
        };
    }

    private static List<String> digits(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(raw.trim().split(""))
            .filter(s -> !s.isBlank())
            .toList();
    }

    private static Set<String> normalizeSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
            .filter(Objects::nonNull)
            .map(NewYorkDrawResultsMapper::normalize)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private enum Slot {
        MIDDAY,
        EVENING,
        UNKNOWN
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NyRow(
        @JsonProperty("draw_date") String drawDate,
        @JsonProperty("midday_daily") String middayDaily,
        @JsonProperty("evening_daily") String eveningDaily,
        @JsonProperty("midday_win_4") String middayWin4,
        @JsonProperty("evening_win_4") String eveningWin4) {}
}

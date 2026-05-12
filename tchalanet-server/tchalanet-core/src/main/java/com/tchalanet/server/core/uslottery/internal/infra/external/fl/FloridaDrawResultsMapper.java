package com.tchalanet.server.core.uslottery.internal.infra.external.fl;

import tools.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.annotation.JsonProperty;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.enums.UsLotteryProvider;
import com.tchalanet.server.common.util.JsonbUtils;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.application.port.out.UsProviderSourceFlags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class FloridaDrawResultsMapper {

    private static final String ORIGIN = "FL_APIM";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var entries = parseEntries(body);
        if (entries.isEmpty()) {
            return UsLotteryProviderResponse.empty(UsLotteryProvider.FL, query);
        }

        var wantedCodes = normalizeSet(query.externalGameCodes());
        var results = new ArrayList<UsLotteryProviderResult>();

        for (var entry : entries) {
            var result = mapEntry(entry, wantedCodes, sourceHash, url, query);
            if (result != null) {
                results.add(result);
            }
        }

        return new UsLotteryProviderResponse(
            UsLotteryProvider.FL,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(results),
            query.includeRaw() ? body : null);
    }

    private UsLotteryProviderResult mapEntry(
        FloridaEntry entry,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        if (entry == null) {
            return null;
        }

        var gameCode = normalizeGameCode(entry.gameName());
        if (gameCode.isBlank()) {
            return null;
        }

        if (!wantedCodes.isEmpty() && !wantedCodes.contains(gameCode)) {
            return null;
        }

        var drawDate = parseFloridaDate(entry.drawDate());
        if (drawDate == null || !query.drawDate().equals(drawDate)) {
            return null;
        }

        var parsed = parseNumbers(entry.drawNumbers(), gameCode);
        if (parsed.main().isEmpty()) {
            return null;
        }

        var expectedSize = expectedSize(gameCode);
        var quality = parsed.main().size() == expectedSize ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var flags =
            new UsProviderSourceFlags(
                ORIGIN,
                sourceHash,
                url,
                Map.of(
                    "provider", UsLotteryProvider.FL.name(),
                    "game_code", gameCode,
                    "draw_date", String.valueOf(drawDate)));

        return new UsLotteryProviderResult(
            gameCode,
            parsed.main(),
            parsed.extras(),
            quality,
            flags,
            null,
            query.includeRaw() ? entry : null);
    }

    private List<FloridaEntry> parseEntries(String body) {
        try {
            JsonNode root = json.readTree(body);

            if (root != null && root.isArray()) {
                return json.fromJson(body, new TypeReference<>() {
                });
            }

            JsonNode dr = root == null ? null : root.get("DrawResults");
            if (dr != null && dr.isArray()) {
                return json.convertValue(dr, new TypeReference<>() {
                });
            }
        } catch (Exception ex) {
            log.warn("fl-client parse failed: {}", ex.getLocalizedMessage(), ex);
        }

        return List.of();
    }

    private static NumberParseResult parseNumbers(List<FloridaNumber> drawNumbers, String gameCode) {
        if (drawNumbers == null || drawNumbers.isEmpty()) {
            return new NumberParseResult(List.of(), List.of(), Map.of());
        }

        int expectedSize = expectedSize(gameCode);

        record NumberInfo(int index, String value) {
        }

        var winningNumbers = new ArrayList<NumberInfo>();
        var extras = new ArrayList<String>();
        var numberAttributes = new LinkedHashMap<String, String>();

        for (FloridaNumber number : drawNumbers) {
            if (number == null) {
                continue;
            }

            var pick = safe(number.numberPick());
            if (pick.isBlank()) {
                continue;
            }

            var type = safe(number.numberType()).toLowerCase(Locale.ROOT);

            if (type.startsWith("wn")) {
                winningNumbers.add(new NumberInfo(parseNumberIndex(type), pick));
            } else {
                extras.add(pick);
                numberAttributes.merge(type.isBlank() ? "extra" : type, pick, (a, b) -> a + "," + b);
            }
        }

        winningNumbers.sort(Comparator.comparingInt(NumberInfo::index));

        var main =
            winningNumbers.stream()
                .map(NumberInfo::value)
                .limit(expectedSize > 0 ? expectedSize : winningNumbers.size())
                .toList();

        return new NumberParseResult(main, List.copyOf(extras), Map.copyOf(numberAttributes));
    }

    private static int expectedSize(String gameCode) {
        return switch (gameCode) {
            case "CASH3", "PICK3", "NUMBERS" -> 3;
            case "CASH4", "PICK4", "WIN4" -> 4;
            default -> 0;
        };
    }

    private static int parseNumberIndex(String type) {
        try {
            return Integer.parseInt(type.substring(2));
        } catch (Exception ex) {
            return 99;
        }
    }

    private static LocalDate parseFloridaDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String datePart = raw.trim();
        if (datePart.length() >= 10) {
            datePart = datePart.substring(0, 10);
        }

        try {
            return LocalDate.parse(datePart, DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));
        } catch (Exception ignored) {
            // try ISO below
        }

        try {
            return LocalDate.parse(datePart);
        } catch (Exception ex) {
            log.warn("Failed to parse FL draw date '{}': {}", raw, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static Set<String> normalizeSet(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
            .filter(Objects::nonNull)
            .map(FloridaDrawResultsMapper::normalizeGameCode)
            .filter(s -> !s.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String normalizeGameCode(String value) {
        return value == null
            ? ""
            : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record NumberParseResult(
        List<String> main,
        List<String> extras,
        Map<String, String> numberAttributes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FloridaEntry(
        @JsonProperty("GameName") String gameName,
        @JsonProperty("DrawDate") String drawDate,
        @JsonProperty("DrawType") String drawType,
        @JsonProperty("DrawNumbers") List<FloridaNumber> drawNumbers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FloridaNumber(
        @JsonProperty("NumberPick") String numberPick,
        @JsonProperty("NumberType") String numberType) {
    }
}

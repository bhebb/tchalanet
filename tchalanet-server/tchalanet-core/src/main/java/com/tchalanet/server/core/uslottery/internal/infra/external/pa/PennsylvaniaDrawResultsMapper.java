package com.tchalanet.server.core.uslottery.internal.infra.external.pa;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Maps the PA DrawingsData.aspx JSON feed. The payload is an array of drawing entries; each Pick
 * entry is the midday draw and nests its evening draw under {@code RelatedEveningDrawing}.
 *
 * <p>{@code DrawingNumbersAsList} holds {@code GameBallCount} digits followed by PA's Wild Ball, so
 * the main numbers are the first {@code GameBallCount} entries and the trailing value is surfaced as
 * an extra. {@code IsMidDayDrawing} selects the slot (MIDDAY / EVENING).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PennsylvaniaDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.PA;
    private static final String ORIGIN = "PA_API";

    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US);
    private static final Pattern MS_DATE = Pattern.compile("/Date\\((-?\\d+)");

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
            // Each top-level entry is one draw; its evening counterpart is nested.
            addIfMatches(entry, wantedCodes, sourceHash, url, query, results);
            addIfMatches(entry.get("RelatedEveningDrawing"), wantedCodes, sourceHash, url, query, results);
        }

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.copyOf(results),
            query.includeRaw() ? body : null);
    }

    private void addIfMatches(
        JsonNode node,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query,
        List<UsLotteryProviderResult> sink) {

        if (node == null || node.isNull()) {
            return;
        }
        try {
            var mapped = mapDrawing(node, wantedCodes, sourceHash, url, query);
            if (mapped != null) {
                sink.add(mapped);
            }
        } catch (Exception ex) {
            log.warn(
                "pa-client skipped invalid entry drawDate={} providerSlotCode={} err={}",
                query.drawDate(), query.providerSlotCode(), ex.getMessage(), ex);
        }
    }

    private UsLotteryProviderResult mapDrawing(
        JsonNode node,
        Set<String> wantedCodes,
        String sourceHash,
        String url,
        UsLotteryProviderQuery query) {

        var gameCode = normalizeGameCode(text(node, "GameName"));
        if (gameCode.isBlank()) {
            return null;
        }
        if (!wantedCodes.isEmpty() && !wantedCodes.contains(gameCode)) {
            return null;
        }

        var slot = isMidday(node) ? "MIDDAY" : "EVENING";
        if (!ProviderSlotCodeMatcher.matches(slot, query.providerSlotCode())) {
            return null;
        }

        var drawDate = resolveDate(node, query);
        if (drawDate == null || !query.drawDate().equals(drawDate)) {
            return null;
        }

        var ballCount = intValue(node.get("GameBallCount"));
        var digits = digits(node.get("DrawingNumbersAsList"));
        if (digits.isEmpty()) {
            return null;
        }

        // DrawingNumbersAsList = GameBallCount main digits + trailing Wild Ball.
        List<String> main;
        List<String> extras;
        if (ballCount > 0 && digits.size() > ballCount) {
            main = List.copyOf(digits.subList(0, ballCount));
            extras = List.copyOf(digits.subList(ballCount, digits.size()));
        } else {
            main = digits;
            extras = List.of();
        }

        var quality = ballCount > 0 && main.size() == ballCount
            ? ResultQuality.COMPLETE
            : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", gameCode);
        metadata.put("draw_date", drawDate.toString());
        metadata.put("provider_slot_code", slot);
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));
        metadata.put("drawing_name", text(node, "DrawingName"));
        if (!extras.isEmpty()) {
            metadata.put("wild_ball", String.join(",", extras));
        }

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));

        return new UsLotteryProviderResult(
            gameCode,
            main,
            extras,
            quality,
            flags,
            query.drawDate().atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? node.toString() : null);
    }

    private List<JsonNode> parseEntries(String body) {
        var nodes = new ArrayList<JsonNode>();
        try {
            JsonNode root = json.readTree(body);
            if (root != null && root.isArray()) {
                root.forEach(nodes::add);
            }
        } catch (Exception ex) {
            log.warn("pa-client parse failed: {}", ex.getMessage(), ex);
        }
        return nodes;
    }

    private LocalDate resolveDate(JsonNode node, UsLotteryProviderQuery query) {
        var ms = node.get("DrawingDate");
        if (ms != null && !ms.isNull()) {
            var matcher = MS_DATE.matcher(ms.asString());
            if (matcher.find()) {
                try {
                    return Instant.ofEpochMilli(Long.parseLong(matcher.group(1)))
                        .atZone(query.timezone()).toLocalDate();
                } catch (Exception ignored) {
                    // fall through to string date
                }
            }
        }
        var raw = text(node, "DrawingDateAsString");
        if (StringUtils.isNotBlank(raw)) {
            try {
                return LocalDate.parse(raw.trim(), MDY);
            } catch (Exception ex) {
                log.warn("pa-client failed to parse date '{}': {}", raw, ex.getMessage());
            }
        }
        return null;
    }

    private static boolean isMidday(JsonNode node) {
        var flag = node.get("IsMidDayDrawing");
        return flag != null && !flag.isNull() && "true".equalsIgnoreCase(flag.asString());
    }

    private static List<String> digits(JsonNode list) {
        if (list == null || !list.isArray()) {
            return List.of();
        }
        var out = new ArrayList<String>();
        for (var el : list) {
            if (el == null || el.isNull()) {
                continue;
            }
            var v = el.asString().trim();
            if (v.matches("\\d+")) {
                out.add(v);
            }
        }
        return out;
    }

    private static int intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        try {
            return Integer.parseInt(node.asString().trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? "" : value.asString().trim();
    }

    private static String normalizeGameCode(String raw) {
        var n = ProviderSlotCodeMatcher.normalize(raw);
        return switch (n) {
            case "PICK2" -> "PICK2";
            case "PICK3" -> "PICK3";
            case "PICK4" -> "PICK4";
            case "PICK5" -> "PICK5";
            default -> "";
        };
    }

    private static Set<String> normalizeWantedCodes(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }
        return codes.stream()
            .filter(Objects::nonNull)
            .map(PennsylvaniaDrawResultsMapper::normalizeGameCode)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }
}

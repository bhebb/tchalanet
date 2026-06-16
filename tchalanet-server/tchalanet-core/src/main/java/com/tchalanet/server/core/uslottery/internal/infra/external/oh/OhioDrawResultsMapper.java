package com.tchalanet.server.core.uslottery.internal.infra.external.oh;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import com.tchalanet.server.core.uslottery.internal.infra.external.oh.OhioDrawResultsClient.OhGame;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Maps the OH DrawGames JSON feed. Shape validated against real API response 2026-06-16.
 *
 * <p>Root: {@code {"statusCode":200,"data":[...]}}. Each draw: {@code drawDate} (ISO local time),
 * {@code numbers[{value,position}]}, {@code approved}. No slot label — infer MIDDAY (hour<15) vs
 * EVENING (hour≥15) from the local draw time.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OhioDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.OH;
    private static final String ORIGIN = "OH_API";

    // Real root field is "data"; keep legacy candidates for forward-compat.
    private static final List<String> DRAW_LIST_FIELDS =
        List.of("data", "DrawGames", "Draws", "draws", "Results", "results");
    private static final List<String> DATE_FIELDS = List.of("drawDate", "DrawDate", "Date", "date");
    private static final List<String> NUMBERS_FIELDS =
        List.of("numbers", "Numbers", "WinningNumbers", "winningNumbers");
    // Real number objects: {value: int, position: int}
    private static final List<String> NUMBER_VALUE_FIELDS = List.of("value", "Value", "Number", "number");
    private static final List<String> POSITION_FIELDS = List.of("position", "Position");

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body, OhGame game, String sourceHash, String url, UsLotteryProviderQuery query) {

        var draws = drawNodes(body);
        if (draws.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var results = new ArrayList<UsLotteryProviderResult>();
        for (var draw : draws) {
            try {
                var mapped = mapDraw(draw, game, sourceHash, url, query);
                if (mapped != null) {
                    results.add(mapped);
                }
            } catch (Exception ex) {
                log.warn(
                    "oh-client skipped invalid draw game={} drawDate={} err={}",
                    game.outputCode(), query.drawDate(), ex.getMessage(), ex);
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

    private UsLotteryProviderResult mapDraw(
        JsonNode draw, OhGame game, String sourceHash, String url, UsLotteryProviderQuery query) {

        // Skip unapproved draws; absent field = accepted.
        var approvedNode = draw.get("approved");
        if (approvedNode != null && !approvedNode.isNull() && !approvedNode.asBoolean()) {
            return null;
        }

        var drawDate = resolveDate(draw);
        if (drawDate == null || !query.drawDate().equals(drawDate)) {
            return null;
        }

        // No slot field; infer from local hour in the drawDate string.
        var slot = inferSlot(draw);
        if (StringUtils.isNotBlank(query.providerSlotCode())) {
            if (slot.isBlank() || !ProviderSlotCodeMatcher.matches(slot, query.providerSlotCode())) {
                return null;
            }
        }

        var main = resolveNumbers(draw, game.expectedSize());
        if (main.isEmpty()) {
            return null;
        }

        var quality = main.size() == game.expectedSize() ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", game.outputCode());
        metadata.put("draw_date", drawDate.toString());
        metadata.put("provider_slot_code", ProviderSlotCodeMatcher.normalize(slot));
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));

        return new UsLotteryProviderResult(
            game.outputCode(),
            main,
            List.of(),
            quality,
            flags,
            query.drawDate().atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? draw.toString() : null);
    }

    private List<JsonNode> drawNodes(String body) {
        var nodes = new ArrayList<JsonNode>();
        try {
            JsonNode root = json.readTree(body);
            if (root == null) {
                return nodes;
            }
            if (root.isArray()) {
                root.forEach(nodes::add);
                return nodes;
            }
            for (var field : DRAW_LIST_FIELDS) {
                var node = root.get(field);
                if (node != null && node.isArray()) {
                    node.forEach(nodes::add);
                    return nodes;
                }
            }
        } catch (Exception ex) {
            log.warn("oh-client parse failed: {}", ex.getMessage(), ex);
        }
        return nodes;
    }

    private LocalDate resolveDate(JsonNode draw) {
        for (var field : DATE_FIELDS) {
            var node = draw.get(field);
            if (node == null || node.isNull()) {
                continue;
            }
            var parsed = parseDate(node.asString());
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    /** Infers MIDDAY/EVENING from the local hour in the ISO drawDate string (no timezone). */
    private static String inferSlot(JsonNode draw) {
        for (var field : DATE_FIELDS) {
            var node = draw.get(field);
            if (node == null || node.isNull()) {
                continue;
            }
            var s = node.asString().trim();
            if (s.length() >= 16) {
                try {
                    int hour = Integer.parseInt(s.substring(11, 13));
                    return hour < 15 ? "MIDDAY" : "EVENING";
                } catch (Exception ignored) {}
            }
        }
        return "";
    }

    private List<String> resolveNumbers(JsonNode draw, int expectedSize) {
        for (var field : NUMBERS_FIELDS) {
            var node = draw.get(field);
            if (node == null || node.isNull() || !node.isArray()) {
                continue;
            }
            var nums = extractNumbersSorted(node);
            if (!nums.isEmpty()) {
                return nums.size() > expectedSize && expectedSize > 0 ? nums.subList(0, expectedSize) : nums;
            }
        }
        return List.of();
    }

    /** Extracts digit strings from a numbers array, sorted ascending by position field. */
    private List<String> extractNumbersSorted(JsonNode array) {
        record Entry(int pos, String val) {}
        var entries = new ArrayList<Entry>();
        int defaultPos = 0;
        for (var el : array) {
            if (el == null || el.isNull()) {
                continue;
            }
            if (el.isObject()) {
                var v = numberValue(el);
                if (v == null) {
                    continue;
                }
                int pos = defaultPos;
                for (var pf : POSITION_FIELDS) {
                    var pn = el.get(pf);
                    if (pn != null && !pn.isNull()) {
                        try {
                            pos = Integer.parseInt(pn.asString().trim());
                            break;
                        } catch (Exception ignored) {}
                    }
                }
                entries.add(new Entry(pos, v));
                defaultPos++;
            } else {
                var s = el.asString().trim();
                if (s.matches("\\d+")) {
                    entries.add(new Entry(defaultPos++, s));
                }
            }
        }
        entries.sort(java.util.Comparator.comparingInt(Entry::pos));
        return entries.stream().map(Entry::val).toList();
    }

    private static String numberValue(JsonNode el) {
        if (el == null || el.isNull()) {
            return null;
        }
        if (el.isObject()) {
            for (var field : NUMBER_VALUE_FIELDS) {
                var v = el.get(field);
                if (v != null && !v.isNull()) {
                    return v.asString().trim();
                }
            }
            return null;
        }
        var s = el.asString().trim();
        return s.matches("\\d+") ? s : null;
    }

    private static LocalDate parseDate(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        var s = raw.trim();
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception ignored) {
            // try US formats
        }
        for (var fmt : List.of("MM/dd/uuuu", "M/d/uuuu", "MM-dd-uuuu")) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt, Locale.US));
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }
}

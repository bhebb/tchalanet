package com.tchalanet.server.core.uslottery.internal.infra.external.ca;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import com.tchalanet.server.core.uslottery.internal.infra.external.ca.CaliforniaDrawResultsClient.CaGame;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Maps the CA DrawGamePastDrawResults JSON. The payload is a single object with a
 * {@code PreviousDraws} array (plus a {@code MostRecentDraw}); each draw carries an ISO
 * {@code DrawDate} and a position-keyed {@code WinningNumbers} object
 * ({@code {"1":{"Number":"5"},"2":{"Number":"8"},...}}).
 *
 * <p>CA Daily games run twice a day but the feed exposes no slot label; the two same-date draws are
 * distinguished by {@code DrawNumber} (evening draws are sequenced after midday). The query's
 * providerSlotCode therefore selects EVENING = highest DrawNumber, MIDDAY = next; a blank slot takes
 * the most recent draw of the date.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CaliforniaDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.CA;
    private static final String ORIGIN = "CA_API";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body, CaGame game, String sourceHash, String url, UsLotteryProviderQuery query) {

        List<CaDraw> draws;
        try {
            draws = drawsForDate(body, query.drawDate());
        } catch (Exception ex) {
            log.warn("ca-client parse failed: {}", ex.getMessage(), ex);
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        if (draws.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        // Highest DrawNumber first (most recent / evening).
        draws.sort(Comparator.comparingLong(CaDraw::drawNumber).reversed());

        var picked = pickForSlot(draws, query.providerSlotCode());
        if (picked == null) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var main = picked.numbers();
        if (main.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var quality = main.size() == game.expectedSize() ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", game.outputCode());
        metadata.put("game_id", String.valueOf(game.gameId()));
        metadata.put("draw_date", picked.drawDate().toString());
        metadata.put("draw_number", String.valueOf(picked.drawNumber()));
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));

        var result = new UsLotteryProviderResult(
            game.outputCode(),
            main,
            List.of(),
            quality,
            flags,
            query.drawDate().atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? picked.raw() : null);

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.of(result),
            query.includeRaw() ? body : null);
    }

    /** Selects the draw matching the requested slot from the date's draws (sorted newest first). */
    private static CaDraw pickForSlot(List<CaDraw> sorted, String providerSlotCode) {
        var norm = ProviderSlotCodeMatcher.normalize(providerSlotCode);
        if (norm.isBlank()) {
            return sorted.getFirst();
        }
        if (ProviderSlotCodeMatcher.matches("EVENING", norm)) {
            return sorted.getFirst();
        }
        if (ProviderSlotCodeMatcher.matches("MIDDAY", norm)) {
            return sorted.size() > 1 ? sorted.get(1) : sorted.getFirst();
        }
        return sorted.getFirst();
    }

    private List<CaDraw> drawsForDate(String body, LocalDate wanted) {
        var out = new ArrayList<CaDraw>();
        JsonNode root = json.readTree(body);
        if (root == null) {
            return out;
        }
        var list = root.get("PreviousDraws");
        if (list != null && list.isArray()) {
            for (var node : list) {
                var draw = toDraw(node);
                if (draw != null && wanted.equals(draw.drawDate())) {
                    out.add(draw);
                }
            }
        }
        if (out.isEmpty()) {
            var draw = toDraw(root.get("MostRecentDraw"));
            if (draw != null && wanted.equals(draw.drawDate())) {
                out.add(draw);
            }
        }
        return out;
    }

    private static CaDraw toDraw(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        var date = parseDate(text(node, "DrawDate"));
        if (date == null) {
            return null;
        }
        long drawNumber = 0L;
        var dn = node.get("DrawNumber");
        if (dn != null && !dn.isNull()) {
            try {
                drawNumber = Long.parseLong(dn.asString().trim());
            } catch (Exception ignored) {
                drawNumber = 0L;
            }
        }
        return new CaDraw(date, drawNumber, numbers(node.get("WinningNumbers")), node.toString());
    }

    /** Reads the position-keyed WinningNumbers object in ascending key order. */
    private static List<String> numbers(JsonNode winningNumbers) {
        if (winningNumbers == null || !winningNumbers.isObject()) {
            return List.of();
        }
        var byKey = new java.util.TreeMap<Integer, String>();
        winningNumbers.propertyStream().forEach(e -> {
            int key;
            try {
                key = Integer.parseInt(e.getKey().trim());
            } catch (Exception ex) {
                return;
            }
            var num = e.getValue() == null ? null : e.getValue().get("Number");
            if (num != null && !num.isNull()) {
                var v = num.asString().trim();
                if (v.matches("\\d+")) {
                    byKey.put(key, v);
                }
            }
        });
        return List.copyOf(byKey.values());
    }

    private static LocalDate parseDate(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        var s = raw.trim();
        try {
            return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        var value = node.get(field);
        return value == null || value.isNull() ? "" : value.asString().trim();
    }

    private record CaDraw(LocalDate drawDate, long drawNumber, List<String> numbers, String raw) {}
}

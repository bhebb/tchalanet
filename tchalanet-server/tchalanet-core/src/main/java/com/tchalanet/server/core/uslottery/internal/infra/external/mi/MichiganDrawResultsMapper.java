package com.tchalanet.server.core.uslottery.internal.infra.external.mi;

import com.tchalanet.server.common.json.utils.JsonbUtils;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.core.uslottery.internal.application.model.UsLotteryProvider;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderQuery;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResponse;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsLotteryProviderResult;
import com.tchalanet.server.core.uslottery.internal.application.port.out.UsProviderSourceFlags;
import com.tchalanet.server.core.uslottery.internal.infra.external.ProviderSlotCodeMatcher;
import com.tchalanet.server.core.uslottery.internal.infra.external.mi.MichiganDrawResultsClient.MiGame;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Maps the MI GraphQL response. The endpoint answers with a (possibly batched) JSON array of
 * {@code {"data": {...}}} objects; the relevant element exposes
 * {@code data.winningNumbers.drawNumbersMid|drawNumbersEve} as arrays of integers. The slot to read
 * is chosen from the query's providerSlotCode (MIDDAY -&gt; Mid, EVENING -&gt; Eve).
 *
 * <p>Sample (Daily 4, 2026-06-15): {@code drawNumbersMid:[5,0,5,6]}, {@code drawNumbersEve:[3,6,2,4]}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MichiganDrawResultsMapper {

    private static final UsLotteryProvider PROVIDER = UsLotteryProvider.MI;
    private static final String ORIGIN = "MI_GRAPHQL";

    private static final String MID_FIELD = "drawNumbersMid";
    private static final String EVE_FIELD = "drawNumbersEve";

    private final JsonbUtils json;

    public UsLotteryProviderResponse map(
        String body, MiGame game, String sourceHash, String url, UsLotteryProviderQuery query) {

        JsonNode winningNumbers = winningNumbersNode(body);
        if (winningNumbers == null) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var pending = winningNumbers.get("resultsPending");
        if (pending != null && !pending.isNull() && "true".equalsIgnoreCase(pending.asString())) {
            log.debug("mi-client results pending for game={} drawDate={}", game.outputCode(), query.drawDate());
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var field = resolveField(query.providerSlotCode());
        if (field == null) {
            log.debug(
                "mi-client cannot resolve mid/eve field from providerSlotCode={}", query.providerSlotCode());
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var main = extractNumbers(winningNumbers.get(field), game.expectedSize());
        if (main.isEmpty()) {
            return UsLotteryProviderResponse.empty(PROVIDER, query);
        }

        var quality = main.size() == game.expectedSize() ? ResultQuality.COMPLETE : ResultQuality.SUSPECT;

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("provider", PROVIDER.name());
        metadata.put("game_code", game.outputCode());
        metadata.put("operation", game.operation());
        metadata.put("draw_date", query.drawDate().toString());
        metadata.put("slot_field", field);
        metadata.put("expected_provider_slot_code", ProviderSlotCodeMatcher.normalize(query.providerSlotCode()));

        var flags = new UsProviderSourceFlags(ORIGIN, sourceHash, url, Map.copyOf(metadata));

        var result = new UsLotteryProviderResult(
            game.outputCode(),
            main,
            List.of(),
            quality,
            flags,
            query.drawDate().atTime(query.drawTime()).atZone(query.timezone()).toInstant(),
            query.includeRaw() ? body : null);

        return new UsLotteryProviderResponse(
            PROVIDER,
            query.drawDate(),
            query.drawTime(),
            query.timezone(),
            List.of(result),
            query.includeRaw() ? body : null);
    }

    /**
     * Finds the {@code winningNumbers} node. The payload is a (possibly batched) array of
     * {@code {"data": {...}}} objects; the winning numbers live under the element whose {@code data}
     * carries a {@code winningNumbers} child. A single (non-batched) object is also tolerated.
     */
    private JsonNode winningNumbersNode(String body) {
        try {
            JsonNode root = json.readTree(body);
            if (root == null) {
                return null;
            }
            if (root.isArray()) {
                for (var element : root) {
                    var wn = winningNumbersFromEnvelope(element);
                    if (wn != null) {
                        return wn;
                    }
                }
                return null;
            }
            return winningNumbersFromEnvelope(root);
        } catch (Exception ex) {
            log.warn("mi-client parse failed: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static JsonNode winningNumbersFromEnvelope(JsonNode envelope) {
        if (envelope == null || envelope.isNull()) {
            return null;
        }
        var data = envelope.has("data") ? envelope.get("data") : envelope;
        if (data == null || data.isNull()) {
            return null;
        }
        var wn = data.get("winningNumbers");
        return wn == null || wn.isNull() ? null : wn;
    }

    private static String resolveField(String providerSlotCode) {
        var norm = ProviderSlotCodeMatcher.normalize(providerSlotCode);
        if (ProviderSlotCodeMatcher.matches("MIDDAY", norm)) {
            return MID_FIELD;
        }
        if (ProviderSlotCodeMatcher.matches("EVENING", norm)) {
            return EVE_FIELD;
        }
        return null;
    }

    private static List<String> extractNumbers(JsonNode node, int expectedSize) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        var out = new ArrayList<String>();
        if (node.isArray()) {
            for (var el : node) {
                if (el == null || el.isNull()) {
                    continue;
                }
                var v = el.asString().trim();
                if (v.matches("\\d+")) {
                    out.add(v);
                }
            }
        } else {
            for (var part : node.asString().split("[\\s\\-,]+")) {
                if (part.matches("\\d+")) {
                    out.add(part);
                }
            }
        }
        if (out.size() > expectedSize && expectedSize > 0) {
            return out.subList(0, expectedSize);
        }
        return out;
    }
}

package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.haiti.api.HaitiFlags;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.haiti.internal.domain.lottery.exception.InvalidExternalPickException;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HaitiProjectionService {

    private final HaitiLotteryPort haitiPort;
    private final HaitiProjectionConfigPort haitiConfigPort;
    private final JsonUtils jsonUtils;
    private final MeterRegistry meterRegistry;

    public HaitiProjectionResult project(
        ResultSlotView slot,
        LocalDate date,
        ResolvedExternalResults external
    ) {
        String pick3 = "";
        String pick4 = "";

        try {
            pick3 = external.hasPick3() ? String.join("", external.pick3().main()) : "";
            pick4 = external.hasPick4() ? String.join("", external.pick4().main()) : "";

            var projCfg = haitiConfigPort.resolve(slot.projectionCfg());
            var pick = ExternalPick.of(pick3, pick4);
            var hp = haitiPort.projectResult(pick, projCfg);

            return new HaitiProjectionResult(
                coerceHaitiLots(hp == null ? null : hp.result()),
                hp == null
                    ? HaitiFlags.fail(1, "PROJECTION_NULL", Map.of())
                    : hp.flags());

        } catch (InvalidExternalPickException e) {
            log.warn(
                "draw-results.fetch invalid_pick slot={} date={} pick3='{}' pick3Length={} pick4='{}' pick4Length={} hasPick3={} hasPick4={} err={}",
                slot.slotKey(),
                date,
                safePick(pick3),
                pick3.length(),
                safePick(pick4),
                pick4.length(),
                external.hasPick3(),
                external.hasPick4(),
                e.getMessage(),
                e
            );

            meterRegistry
                .counter(
                    "draw_external_pick_invalid_total",
                    "slot", slot.slotKey(),
                    "reason", invalidReason(pick3, pick4)
                )
                .increment();

            return new HaitiProjectionResult(
                emptyHaitiLots(),
                HaitiFlags.fail(
                    1,
                    "INVALID_EXTERNAL_PICK",
                    Map.of(
                        "error", String.valueOf(e.getMessage()),
                        "pick3", safePick(pick3),
                        "pick3Length", pick3.length(),
                        "pick4", safePick(pick4),
                        "pick4Length", pick4.length()
                    )
                ));

        } catch (Exception e) {
            log.warn(
                "draw-results.fetch projection_failed slot={} date={} pick3='{}' pick4='{}' err={}",
                slot.slotKey(),
                date,
                safePick(pick3),
                safePick(pick4),
                e.getMessage(),
                e
            );

            return new HaitiProjectionResult(
                emptyHaitiLots(),
                HaitiFlags.fail(
                    1,
                    "PROJECTION_EXCEPTION",
                    Map.of("error", String.valueOf(e.getMessage()))));
        }
    }

    private String safePick(String value) {
        if (value == null) {
            return "";
        }

        // Garde seulement une valeur courte et lisible dans les logs.
        // Les picks sont censés être 3/4 digits, donc pas besoin de logger plus.
        return value.length() <= 12 ? value : value.substring(0, 12) + "...";
    }

    private String invalidReason(String pick3, String pick4) {
        if (pick3 == null || pick3.isBlank()) {
            return "pick3_missing";
        }
        if (!pick3.matches("\\d{3}")) {
            return "pick3_invalid_format";
        }
        if (pick4 == null || pick4.isBlank()) {
            return "pick4_missing";
        }
        if (!pick4.matches("\\d{4}")) {
            return "pick4_invalid_format";
        }
        return "unknown";
    }

    private ObjectNode coerceHaitiLots(Object anyResult) {
        var out = jsonUtils.emptyObject();

        if (anyResult == null) {
            return emptyHaitiLots();
        }

        var node = jsonUtils.toJsonNode(anyResult);
        if (node == null || node.isNull() || !node.isObject()) {
            return emptyHaitiLots();
        }

        var obj = (ObjectNode) node;
        var lots = obj.get("lots");

        if (lots != null && lots.isObject()) {
            out.put("lot1", text(lots, "LOT1"));
            out.put("lot2", text(lots, "LOT2"));
            out.put("lot3", text(lots, "LOT3"));
            out.put("lot4", text(lots, "LOT4"));
            return out;
        }

        out.put("lot1", text(obj, "lot1"));
        out.put("lot2", text(obj, "lot2"));
        out.put("lot3", text(obj, "lot3"));
        out.put("lot4", text(obj, "lot4"));
        return out;
    }

    private String text(JsonNode node, String field) {
        var value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private ObjectNode emptyHaitiLots() {
        var o = jsonUtils.emptyObject();
        o.put("lot1", "");
        o.put("lot2", "");
        o.put("lot3", "");
        o.put("lot4", "");
        return o;
    }
}

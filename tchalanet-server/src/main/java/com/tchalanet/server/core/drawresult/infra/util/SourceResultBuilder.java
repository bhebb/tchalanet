package com.tchalanet.server.core.drawresult.infra.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.common.util.JsonUtils;

import java.time.Instant;
import java.time.LocalDate;

public final class SourceResultBuilder {
    private SourceResultBuilder() {
    }

    public static ObjectNode build(
        JsonUtils jsonUtils,
        String provider,
        String slotKey,
        LocalDate drawDate,
        Instant occurredAt,
        ExternalResultOutput p3,
        ExternalResultOutput p4) {

        var root = jsonUtils.emptyObjectNode();
        root.put("provider", nn(provider));
        root.put("slot_key", nn(slotKey));
        root.put("draw_date", drawDate == null ? "" : drawDate.toString());
        root.put("occurred_at", occurredAt == null ? "" : occurredAt.toString());

        root.set("pick3", pickNode(jsonUtils, p3));
        root.set("pick4", pickNode(jsonUtils, p4));

        root.put("pick3_digits", digits(p3));
        root.put("pick4_digits", digits(p4));

        return root;
    }

    private static ObjectNode pickNode(JsonUtils mapper, ExternalResultOutput out) {
        var n = mapper.emptyObjectNode();
        if (out == null) {
            n.put("found", false);
            n.put("status", "");
            n.put("quality", "SUSPECT");
            n.put("occurred_at", "");
            n.putArray("main");
            n.putArray("extra");
            n.set("source_flags", mapper.emptyObjectNode());
            n.set("raw", mapper.emptyObjectNode());
            return n;
        }

        n.put("found", out.found());
        n.put("status", nn(out.status()));
        n.put("quality", out.quality() == null ? "SUSPECT" : out.quality().name());
        n.put("occurred_at", out.occurredAt() == null ? "" : out.occurredAt().toString());

        var main = n.putArray("main");
        for (var s : out.main()) main.add(s);

        var extra = n.putArray("extra");
        for (var s : out.extra()) extra.add(s);

        n.set("source_flags", mapper.valueToTree(out.sourceFlags()));
        n.set("raw", mapper.valueToTree(out.rawPayload()));
        return n;
    }

    private static String digits(ExternalResultOutput out) {
        if (out == null || !out.found() || out.main().isEmpty()) return "";
        return String.join("", out.main());
    }

    private static String nn(String s) {
        return s == null ? "" : s;
    }
}

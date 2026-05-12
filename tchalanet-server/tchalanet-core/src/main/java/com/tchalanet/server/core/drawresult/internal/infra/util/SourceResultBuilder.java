package com.tchalanet.server.core.drawresult.internal.infra.util;

import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import java.time.Instant;
import java.time.LocalDate;
import tools.jackson.databind.node.ObjectNode;

public final class SourceResultBuilder {

    private SourceResultBuilder() {}

    public static ObjectNode build(
        JsonUtils jsonUtils,
        String provider,
        String slotKey,
        LocalDate drawDate,
        Instant occurredAt,
        ExternalResultItem p3,
        ExternalResultItem p4) {

        var root = jsonUtils.emptyObject();
        root.put("provider", emptyIfNull(provider));
        root.put("slot_key", emptyIfNull(slotKey));
        root.put("draw_date", drawDate == null ? "" : drawDate.toString());
        root.put("occurred_at", occurredAt == null ? "" : occurredAt.toString());

        root.set("pick3", pickNode(jsonUtils, p3));
        root.set("pick4", pickNode(jsonUtils, p4));

        root.put("pick3_digits", digits(p3));
        root.put("pick4_digits", digits(p4));

        return root;
    }

    private static ObjectNode pickNode(JsonUtils jsonUtils, ExternalResultItem item) {
        var n = jsonUtils.emptyObject();

        if (item == null || !item.found()) {
            n.put("found", false);
            n.put("status", "MISSING");
            n.put("quality", "SUSPECT");
            n.put("occurred_at", "");
            n.putArray("main");
            n.putArray("extras");
            n.set("source_flags", jsonUtils.emptyObject());
            return n;
        }

        n.put("found", true);
        n.put("status", "FOUND");
        n.put("game_code", emptyIfNull(item.gameCode()));
        n.put("quality", item.quality() == null ? "SUSPECT" : item.quality().name());
        n.put("occurred_at", item.occurredAt() == null ? "" : item.occurredAt().toString());

        var main = n.putArray("main");
        if (item.main() != null) {
            for (var digit : item.main()) {
                main.add(emptyIfNull(digit));
            }
        }

        var extras = n.putArray("extras");
        if (item.extras() != null) {
            for (var extra : item.extras()) {
                extras.add(emptyIfNull(extra));
            }
        }

        n.set("source_flags", jsonUtils.toJsonNode(item.sourceFlags()));
        return n;
    }

    private static String digits(ExternalResultItem item) {
        if (item == null || !item.found() || item.main() == null || item.main().isEmpty()) {
            return "";
        }
        return String.join("", item.main());
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}

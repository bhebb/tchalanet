package com.tchalanet.server.core.drawresult.internal.application.service;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalResultItem;
import com.tchalanet.server.core.drawresult.internal.infra.util.SourceResultBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DrawResultPersistenceAssembler {

    private final JsonUtils jsonUtils;

    public DrawResultPersistPayload assemble(
        ResultSlotView slot,
        LocalDate date,
        Instant occurredAt,
        ResolvedExternalResults external,
        HaitiProjectionResult projection,
        boolean includeRaw) {

        var sourceResult =
            SourceResultBuilder.build(
                jsonUtils,
                slot.provider(),
                slot.slotKey(),
                date,
                occurredAt,
                external.pick3(),
                external.pick4());

        var flags = jsonUtils.emptyObject();

        var sourceFlags = jsonUtils.emptyObject();
        if (external.pick3() != null) {
            sourceFlags.set("pick3", jsonUtils.toJsonNode(external.pick3().sourceFlags()));
        }
        if (external.pick4() != null) {
            sourceFlags.set("pick4", jsonUtils.toJsonNode(external.pick4().sourceFlags()));
        }

        flags.set("source", sourceFlags);
        flags.set("haiti", jsonUtils.toJsonNode(projection.flags()));

        return new DrawResultPersistPayload(
            sourceResult,
            projection.haitiResult(),
            includeRaw ? buildRawPayload(external) : null,
            flags,
            resolveQuality(external.pick3(), external.pick4()),
            combinedSourceHash(slot.slotKey(), date, external.pick3(), external.pick4()));
    }

    private ObjectNode buildRawPayload(ResolvedExternalResults external) {
        var raw = jsonUtils.emptyObject();

        if (external.pick3() != null) {
            raw.set("pick3_raw", jsonUtils.toJsonNode(external.pick3().rawPayload()));
        }

        if (external.pick4() != null) {
            raw.set("pick4_raw", jsonUtils.toJsonNode(external.pick4().rawPayload()));
        }

        if (external.rawPayload() != null) {
            raw.set("provider_payload", jsonUtils.toJsonNode(external.rawPayload()));
        }

        return raw;
    }

    private String resolveQuality(ExternalResultItem p3, ExternalResultItem p4) {
        if (p3 != null && p3.found() && p3.quality() != null) {
            return p3.quality().name();
        }
        if (p4 != null && p4.found() && p4.quality() != null) {
            return p4.quality().name();
        }
        return null;
    }

    private String combinedSourceHash(
        String slotKey,
        LocalDate date,
        ExternalResultItem p3,
        ExternalResultItem p4) {

        String p3Hash = p3 == null || p3.sourceFlags() == null ? "" : p3.sourceFlags().sourceHash();
        String p4Hash = p4 == null || p4.sourceFlags() == null ? "" : p4.sourceFlags().sourceHash();

        return DigestUtils.sha256Hex(slotKey + "|" + date + "|p3=" + p3Hash + "|p4=" + p4Hash);
    }
}

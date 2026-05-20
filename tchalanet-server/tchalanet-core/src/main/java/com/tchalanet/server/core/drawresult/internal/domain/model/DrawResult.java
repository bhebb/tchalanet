package com.tchalanet.server.core.drawresult.internal.domain.model;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;

/**
 * DrawResult (global, canonique)
 *
 * <p>Unicité: (result_slot_id, occurred_at). Contient le résultat source normalisé + la
 * projection haïtienne (lot1..lot4).
 */
public record DrawResult(
    Instant occurredAt, // instant du tirage côté source
    DrawResultStatus status, // PROVISIONAL/CONFIRMED/OVERRIDDEN/ERROR
    DrawSource source,       // EXTERNAL/MANUAL/ADMIN_OVERRIDE/...
    ResultQuality quality, // optional
    String sourceHash, // optional
    Instant fetchedAt, // required
    JsonNode sourceResult, // required (json normalisé pick3/pick4)
    JsonNode haitiResult, // required (json lots HA)
    JsonNode rawPayload, // optional
    String overrideReason // optional
) {

    public DrawResult {
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(status);
        Objects.requireNonNull(source);
        Objects.requireNonNull(fetchedAt);
    }


    public boolean overridden() {
        return overrideReason != null && !overrideReason.isBlank();
    }

    public DrawResult override(JsonNode newHaiti, String reason) {
        return new DrawResult(
            occurredAt,
            status,
            source,
            quality,
            sourceHash,
            fetchedAt,
            sourceResult,
            newHaiti,
            rawPayload,
            reason);
    }
}

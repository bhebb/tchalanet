package com.tchalanet.server.core.draw.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a generic external draw result, independent of the source provider. This is a Value
 * Object used to transfer data from external integrations to the Draw domain.
 */
public record ExternalDrawResult(
    String providerCode, // e.g., "US_FL", "US_NY", "HT_LOTTO"
    String externalKey, // Unique key from the external provider for the draw type
    String channelCode, // Our internal draw channel code (e.g., "US_FL_NUM3_MID")
    UUID tenantId, // The tenant this result is for
    Instant scheduledAt, // The scheduled time of the draw
    List<String> numbersRaw, // Raw winning numbers (e.g., ["1", "2", "3"])
    Instant occurredAt,
    String rawPayload, // ou JsonNode si tu veux
    Map<String, Object> extraData // Any additional data from the provider
    ) {}

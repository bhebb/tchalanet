package com.tchalanet.server.draw.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a generic external draw result, independent of the source provider. This is a Value
 * Object used to transfer data from external integrations to the Draw domain.
 */
public record ExternalDrawResult(
    String providerCode, // e.g., "US_FL", "US_NY", "HT_LOTTO"
    String externalKey, // Unique key from the external provider for the draw type
    String channelCode, // Our internal draw channel code (e.g., "US_FL_PICK3_MID")
    UUID tenantId, // The tenant this result is for
    Instant scheduledAt, // The scheduled time of the draw
    List<String> numbersRaw, // Raw winning numbers (e.g., ["1", "2", "3"])
    Map<String, Object> extraData // Any additional data from the provider
    ) {
  public ExternalDrawResult {
    Objects.requireNonNull(providerCode, "Provider code cannot be null");
    Objects.requireNonNull(externalKey, "External key cannot be null");
    Objects.requireNonNull(channelCode, "Channel code cannot be null");
    Objects.requireNonNull(tenantId, "Tenant ID cannot be null");
    Objects.requireNonNull(scheduledAt, "Scheduled at cannot be null");
    Objects.requireNonNull(numbersRaw, "Numbers raw cannot be null");
  }
}

package com.tchalanet.server.core.limitpolicy.api.model.offline;

import com.tchalanet.server.common.types.money.Money;

import java.time.Duration;

/**
 * Tenant offline policy: bounds within which an {@code OfflineGrant} can be issued and
 * within which submissions remain syncable.
 *
 * @param offlineEnabled         tenant allows offline sales
 * @param batchSize              number of {@code OfflineCode} rows allocated per batch
 * @param validityDuration       window during which the device may produce offline sales
 * @param syncAcceptedExtension  extra grace window after {@code validUntil} during which the
 *                               server still accepts late uploads
 * @param maxTicketCount         hard cap on validated submissions per grant
 * @param maxTotalAmount         hard cap on cumulative validated stake per grant
 */
public record OfflineLimitPolicy(
    boolean offlineEnabled,
    int batchSize,
    Duration validityDuration,
    Duration syncAcceptedExtension,
    int maxTicketCount,
    Money maxTotalAmount
) {

    public OfflineLimitPolicy {
        if (batchSize <= 0) throw new IllegalArgumentException("batchSize must be > 0");
        if (validityDuration.isZero() || validityDuration.isNegative())
            throw new IllegalArgumentException("validityDuration must be > 0");
        if (syncAcceptedExtension.isNegative())
            throw new IllegalArgumentException("syncAcceptedExtension cannot be negative");
        if (maxTicketCount <= 0)
            throw new IllegalArgumentException("maxTicketCount must be > 0");
    }
}

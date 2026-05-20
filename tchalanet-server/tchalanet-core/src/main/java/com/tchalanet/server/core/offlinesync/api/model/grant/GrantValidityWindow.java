package com.tchalanet.server.core.offlinesync.api.model.grant;

import java.time.Instant;

/**
 * Time window in which an offline grant is usable.
 *
 * <p>{@code validUntil} bounds local sale creation; {@code syncAcceptedUntil} extends the
 * window during which the server still accepts submissions for sales already created locally.
 */
public record GrantValidityWindow(
    Instant validFrom,
    Instant validUntil,
    Instant syncAcceptedUntil
) {
    public GrantValidityWindow {
        if (validFrom == null || validUntil == null || syncAcceptedUntil == null)
            throw new IllegalArgumentException("GrantValidityWindow fields are required");
        if (!validFrom.isBefore(validUntil))
            throw new IllegalArgumentException("validFrom must be before validUntil");
        if (validUntil.isAfter(syncAcceptedUntil))
            throw new IllegalArgumentException("validUntil must be <= syncAcceptedUntil");
    }

    public boolean containsForSale(Instant clientSoldAt) {
        return !clientSoldAt.isBefore(validFrom) && clientSoldAt.isBefore(validUntil);
    }

    public boolean acceptsSyncAt(Instant receivedAt) {
        return !receivedAt.isAfter(syncAcceptedUntil);
    }
}

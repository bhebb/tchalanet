package com.tchalanet.server.core.draw.api.query;

import java.time.Instant;

/**
 * Public-safe view of an upcoming draw for the cashier dashboard widget.
 * Does not expose internal IDs (DrawId, DrawChannelId, TenantId).
 */
public record CashierNextDrawView(
    String channelCode,
    String channelLabel,
    Instant scheduledAt,
    Instant cutoffAt,
    String status
) {}

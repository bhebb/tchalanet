package com.tchalanet.server.core.offlinesync.api.command.grant;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;

import java.time.Instant;

/**
 * Public-safe snapshot of a draw embedded in {@link RequestOfflineGrantResult}. The POS
 * device caches this list and pins one {@code drawId} on every offline sale.
 */
public record OfflineUpcomingDrawSnapshot(
    DrawId drawId,
    DrawChannelId drawChannelId,
    String drawChannelCode,
    String drawChannelLabel,
    Instant scheduledAt,
    Instant cutoffAt,
    String status
) {}

package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantGameId;
import java.time.LocalTime;

/**
 * Calendar row used by GenerateDrawsForRange.
 *
 * <p>active: channel-level toggle (ops can disable MID/EVE independently) enabled:
 * tenant-game/channel activation (via draw_channel_game) dependsOnChannelId: optional sequencing
 * constraint (e.g. EVE depends on MID)
 */
public record DrawChannelCalendarRow(
    DrawChannelId channelId,
    TenantGameId tenantGameId,
    String code,
    String timezone,
    LocalTime drawTime,
    int cutoffSec,
    String daysOfWeek,
    String defaultSource,
    boolean active,
    boolean enabled,
    int sortOrder,
    DrawChannelId dependsOnChannelId) {}

package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import java.time.ZonedDateTime;
import java.util.Map;

public record DrawSummaryView(
    DrawId id,
    String channelCode,
    String channelName,
    ZonedDateTime scheduledAt,
    ZonedDateTime cutoffTime,
    DrawStatus status,
    boolean next,
    boolean active,
    Map<String, Object> lastResult
) {}

package com.tchalanet.server.core.draw.domain.model;
import com.tchalanet.server.common.types.id.DrawId;

import java.time.ZonedDateTime;
import java.util.UUID;

public record DrawSummary(
    DrawId id, String channelCode, ZonedDateTime scheduledAt, DrawStatus status) {}

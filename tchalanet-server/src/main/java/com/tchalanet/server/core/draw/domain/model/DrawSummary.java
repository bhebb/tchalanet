package com.tchalanet.server.core.draw.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record DrawSummary(
    UUID id, String channelCode, ZonedDateTime scheduledAt, DrawStatus status) {}

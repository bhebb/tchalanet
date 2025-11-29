package com.tchalanet.server.core.draw.application.query.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record GetNextDrawQuery(UUID tenantId, String channelCode, ZonedDateTime now) {}

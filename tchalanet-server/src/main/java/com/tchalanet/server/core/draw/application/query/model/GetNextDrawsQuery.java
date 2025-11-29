package com.tchalanet.server.core.draw.application.query.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public record GetNextDrawsQuery(UUID tenantId, ZonedDateTime now, int limit) {}

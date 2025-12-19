package com.tchalanet.server.core.draw.application.query.projection;

import java.time.Instant;
import java.util.UUID;

public record OpenableDrawRow(UUID tenantId, UUID drawId, boolean locked, Instant scheduledAt, int cutoffSec) {}

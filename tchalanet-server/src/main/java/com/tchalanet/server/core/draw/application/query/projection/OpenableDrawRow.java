package com.tchalanet.server.core.draw.application.query.projection;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record OpenableDrawRow(
    TenantId tenantId, DrawId drawId, boolean locked, Instant scheduledAt, Instant cutoffAt) {}

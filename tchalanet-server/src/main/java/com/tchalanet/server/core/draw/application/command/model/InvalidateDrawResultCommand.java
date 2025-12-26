package com.tchalanet.server.core.draw.application.command.model;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.UUID;

public record InvalidateDrawResultCommand(
    DrawId drawId, TenantId tenantId, UUID performedBy, Instant performedAt, String reason) {}

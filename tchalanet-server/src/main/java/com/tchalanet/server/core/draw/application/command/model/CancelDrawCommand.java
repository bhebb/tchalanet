package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record CancelDrawCommand(
    TenantId tenantId, DrawId drawId, UUID cancelledBy, String reason) {}

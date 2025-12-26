package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.UUID;

public record RetrySettleDrawCommand(TenantId tenantId, DrawId drawId, UUID triggeredBy) {}

package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public record SettleDrawCommand(TenantId tenantId, DrawId drawId) {}

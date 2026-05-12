package com.tchalanet.server.core.draw.api.command;

import com.tchalanet.server.common.types.id.TenantId;

public record RefreshDrawCacheCommand(TenantId tenantId) {}

package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;

public record InvalidateDrawCacheCommand(TenantId tenantId) {}

package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public record DrawQuery(TenantId tenantId, DrawId drawId) {}

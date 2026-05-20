package com.tchalanet.server.core.draw.internal.application.query.projection;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public record DueToCloseRow(TenantId tenantId, DrawId drawId, boolean locked) {}

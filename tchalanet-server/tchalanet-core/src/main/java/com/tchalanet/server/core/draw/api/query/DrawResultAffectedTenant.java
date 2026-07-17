package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public record DrawResultAffectedTenant(
    TenantId tenantId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    String drawChannelCode,
    String drawChannelLabel,
    String resultSlotKey) {}

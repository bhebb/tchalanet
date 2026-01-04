package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public record GetDrawChannelQuery(TenantId tenantId, DrawChannelId id)
    implements Query<DrawChannel> {}

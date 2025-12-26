package com.tchalanet.server.core.draw.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.domain.model.DrawChannelSummary;
import java.util.List;
import java.util.UUID;

public record ListActiveDrawChannelsQuery(TenantId tenantId)
    implements Query<List<DrawChannelSummary>> {}

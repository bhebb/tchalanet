package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.util.List;

public record ListLastDaysDrawResultsQuery(TenantId tenantId, String channelCode, int days)
    implements Query<List<DrawResult>> {}

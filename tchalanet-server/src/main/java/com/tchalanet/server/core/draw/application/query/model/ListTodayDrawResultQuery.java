package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import java.util.List;

public record ListTodayDrawResultQuery(
    TenantId tenantId, String channelCode, Integer page, Integer size)
    implements Query<List<DrawResult>> {}

package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.domain.model.DrawSummary;
import java.time.LocalDate;
import java.util.List;

public record ListDrawsQuery(TenantId tenantId, String channelCode, LocalDate from, LocalDate to)
    implements Query<List<DrawSummary>> {}

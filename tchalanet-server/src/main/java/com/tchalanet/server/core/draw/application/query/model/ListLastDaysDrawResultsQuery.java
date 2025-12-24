package com.tchalanet.server.core.draw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.draw.domain.model.DrawResult;

import java.util.List;
import java.util.UUID;

public record ListLastDaysDrawResultsQuery(UUID tenantId, String channelCode, int days) implements Query<List<DrawResult>> {}

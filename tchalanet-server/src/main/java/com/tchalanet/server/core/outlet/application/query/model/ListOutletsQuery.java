package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;
import java.util.List;

public record ListOutletsQuery(UUID tenantId) implements Query<List<com.tchalanet.server.core.outlet.domain.model.Outlet>> {}

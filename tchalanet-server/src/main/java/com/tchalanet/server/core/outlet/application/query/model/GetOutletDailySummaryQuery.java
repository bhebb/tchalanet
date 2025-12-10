package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import java.util.UUID;
import java.time.LocalDate;

public record GetOutletDailySummaryQuery(UUID tenantId, UUID outletId, LocalDate date) implements Query<java.util.Map<String,Object>> {}


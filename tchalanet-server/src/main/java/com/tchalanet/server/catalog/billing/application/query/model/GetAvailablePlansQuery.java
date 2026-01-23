package com.tchalanet.server.catalog.billing.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.catalog.billing.domain.model.Plan;
import java.util.List;

public record GetAvailablePlansQuery() implements Query<List<Plan>> {}

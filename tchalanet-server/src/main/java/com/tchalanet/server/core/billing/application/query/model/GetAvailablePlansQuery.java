package com.tchalanet.server.core.billing.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.billing.domain.model.Plan;
import java.util.List;

public record GetAvailablePlansQuery() implements Query<List<Plan>> {}

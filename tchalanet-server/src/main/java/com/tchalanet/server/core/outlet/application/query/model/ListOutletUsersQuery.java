package com.tchalanet.server.core.outlet.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import java.util.List;

public record ListOutletUsersQuery(OutletId outletId) implements Query<List<OutletUserView>> {}

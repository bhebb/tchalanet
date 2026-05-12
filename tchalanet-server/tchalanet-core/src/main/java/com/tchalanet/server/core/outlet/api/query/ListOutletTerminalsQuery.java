package com.tchalanet.server.core.outlet.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.OutletId;
import java.util.List;

public record ListOutletTerminalsQuery(OutletId outletId)
    implements Query<List<OutletTerminalView>> {}

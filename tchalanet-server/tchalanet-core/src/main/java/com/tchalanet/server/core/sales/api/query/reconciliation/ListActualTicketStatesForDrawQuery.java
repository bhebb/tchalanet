package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import java.util.List;

public record ListActualTicketStatesForDrawQuery(
    DrawId drawId
) implements Query<List<ActualTicketStateRow>> {}

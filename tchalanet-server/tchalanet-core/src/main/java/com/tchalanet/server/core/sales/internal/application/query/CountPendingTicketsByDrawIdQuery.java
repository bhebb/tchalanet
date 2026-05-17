package com.tchalanet.server.core.sales.internal.application.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

public record CountPendingTicketsByDrawIdQuery(DrawId drawId) implements Query<Long> {
}

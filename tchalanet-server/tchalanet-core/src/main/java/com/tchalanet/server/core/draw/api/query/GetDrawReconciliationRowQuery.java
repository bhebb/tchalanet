package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;

public record GetDrawReconciliationRowQuery(
    DrawId drawId
) implements Query<ReconciliationDrawRow> {}

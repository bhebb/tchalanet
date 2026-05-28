package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawResultId;
import java.util.List;

public record ListDrawsForDrawResultQuery(
    DrawResultId drawResultId
) implements Query<List<ReconciliationDrawRow>> {}

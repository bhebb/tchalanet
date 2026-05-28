package com.tchalanet.server.core.sales.api.query.reconciliation;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawResultId;
import java.util.List;

public record ListExpectedTicketOutcomesForDrawResultQuery(
    DrawResultId drawResultId
) implements Query<List<ExpectedTicketOutcomeRow>> {}

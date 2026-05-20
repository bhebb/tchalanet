package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.UserId;

import java.util.List;

public record ListCashierPendingApprovalsQuery(
    UserId cashierId,
    int limit
) implements Query<List<CashierPendingApprovalView>> {}

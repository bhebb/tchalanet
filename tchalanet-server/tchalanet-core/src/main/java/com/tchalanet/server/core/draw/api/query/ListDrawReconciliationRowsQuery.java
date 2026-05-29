package com.tchalanet.server.core.draw.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record ListDrawReconciliationRowsQuery(
    TenantId tenantId,
    LocalDate businessDate
) implements Query<List<ReconciliationDrawRow>> {}

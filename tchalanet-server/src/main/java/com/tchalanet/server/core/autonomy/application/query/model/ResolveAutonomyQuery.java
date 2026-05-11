package com.tchalanet.server.core.autonomy.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import jakarta.validation.constraints.NotNull;

public record ResolveAutonomyQuery(
    @NotNull TenantId tenantId,
    OutletId outletId,
    UserId userId,
    @NotNull BreachOutcome riskOutcome
) implements Query<ResolveAutonomyView> {}

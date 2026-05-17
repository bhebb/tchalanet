package com.tchalanet.server.core.sales.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.api.model.view.TicketForDrawSettlementView;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record
GetTicketForDrawSettlementQuery(
    @NotNull TenantId tenantId,
    @NotNull DrawId drawId
) implements Query<List<TicketForDrawSettlementView>> {}

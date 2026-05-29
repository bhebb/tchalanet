package com.tchalanet.server.core.outlet.api.command.zone;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;

public record CreateSalesZoneCommand(
    TenantId tenantId,
    String code,
    String label,
    SalesZoneId parentId)
    implements Command<SalesZoneId> {}

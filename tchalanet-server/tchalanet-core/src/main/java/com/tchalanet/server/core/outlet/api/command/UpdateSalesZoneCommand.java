package com.tchalanet.server.core.outlet.api.command.zone;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.SalesZoneId;
import com.tchalanet.server.common.types.id.TenantId;

public record UpdateSalesZoneCommand(
    TenantId tenantId,
    SalesZoneId zoneId,
    String label,
    Boolean active)
    implements Command<Void> {}

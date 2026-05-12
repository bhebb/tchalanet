package com.tchalanet.server.core.outlet.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

/** Update outlet configuration command */
public record UpdateOutletConfigCommand(
    TenantId tenantId, OutletId outletId, OutletConfigPatch patch) implements Command<Void> {}

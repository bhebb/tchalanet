package com.tchalanet.server.core.offlinesync.internal.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;

public record DispatchReadyOfflineSubmissionsCommand(
    TenantId tenantId
) implements Command<Integer> {}

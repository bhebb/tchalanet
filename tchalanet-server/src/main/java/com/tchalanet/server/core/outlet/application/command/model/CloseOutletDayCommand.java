package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.bus.Command;

/** CloseOutletDayCommand now wraps a single payload object instead of separate params. */
public record CloseOutletDayCommand(TenantId tenantId, OutletId outletId, CloseOutletDayPayload payload) implements Command<Void> {}


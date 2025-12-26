package com.tchalanet.server.core.outlet.application.command.model;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;
import java.time.LocalDate;

public record ReopenOutletDayCommand(TenantId tenantId, OutletId outletId, LocalDate date) implements Command<Void> {}


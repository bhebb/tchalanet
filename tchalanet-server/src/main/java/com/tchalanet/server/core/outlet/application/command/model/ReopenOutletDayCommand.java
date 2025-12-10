package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;
import java.time.LocalDate;

public record ReopenOutletDayCommand(UUID tenantId, UUID outletId, LocalDate date) implements Command<Void> {}


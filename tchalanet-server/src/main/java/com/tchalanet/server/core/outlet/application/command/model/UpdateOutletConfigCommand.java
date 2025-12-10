package com.tchalanet.server.core.outlet.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.util.UUID;
import java.util.Map;

/** Change outlet configuration (limits, options) */
public record UpdateOutletConfigCommand(UUID tenantId, UUID outletId, Map<String,Object> config) implements Command<Void> {}


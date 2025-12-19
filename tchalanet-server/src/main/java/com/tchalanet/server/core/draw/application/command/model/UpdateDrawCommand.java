package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDrawCommand(UUID tenantId, UUID drawId, LocalDate scheduledDate, String code, String name)
    implements Command<com.tchalanet.server.core.draw.domain.model.Draw> {}

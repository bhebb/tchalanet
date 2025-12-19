package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.draw.domain.model.Draw;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDrawCommand(UUID tenantId, String channelCode, LocalDate scheduledDate) implements Command<Draw> {}

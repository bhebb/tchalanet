package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record UpdateDrawCommand(
    TenantId tenantId, DrawId drawId, LocalDate scheduledDate)
    implements Command<com.tchalanet.server.core.draw.domain.model.Draw> {}

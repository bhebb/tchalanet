package com.tchalanet.server.core.draw.application.command.model;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Command;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDrawCommand(TenantId tenantId, DrawId drawId, LocalDate scheduledDate, String code, String name)
    implements Command<com.tchalanet.server.core.draw.domain.model.Draw> {}

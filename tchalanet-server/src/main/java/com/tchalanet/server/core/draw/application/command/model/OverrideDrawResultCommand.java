package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OverrideDrawResultCommand(
    DrawId drawId,
    TenantId tenantId,
    UUID adminId,
    Instant overriddenAt,
    List<String> numbersMain,
    List<String> numbersExtra,
    String reason)
    implements Command<Void> {}

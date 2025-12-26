package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecordManualDrawResultCommand(
    DrawId drawId,
    TenantId tenantId,
    UUID performedBy,
    Instant performedAt,
    List<String> numbersMain,
    List<String> numbersExtra,
    String reason) {}

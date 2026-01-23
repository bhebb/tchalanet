package com.tchalanet.server.catalog.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record OverrideDrawResultCommand(
    TenantId tenantId,
    String slotKey, // ex: NY_MID
    LocalDate drawDate, // date du slot (locale slot)
    String pick3,
    String pick4,
    String reason,
    boolean force // true si tu veux écraser même FINAL (sinon bloque)
    ) implements Command<OverrideDrawResultResult> {}

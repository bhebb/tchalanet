package com.tchalanet.server.catalog.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record RecordManualDrawResultCommand(
    TenantId tenantId,
    LocalDate drawDate,
    String slotKey, // ✅ ex: NY_MID
    String recordedBy,
    String notes,
    String pick3,
    String pick4,
    boolean force // ✅ même logique que override: permet écraser FINAL
    ) implements Command<RecordManualDrawResultResult> {}

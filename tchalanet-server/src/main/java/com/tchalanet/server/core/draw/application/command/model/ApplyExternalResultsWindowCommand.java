package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record ApplyExternalResultsWindowCommand(
    TenantId tenantId,
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys,
    boolean force,
    boolean dryRun,
    int maxSlots)
    implements Command<ApplyExternalResultsWindowResult> {}

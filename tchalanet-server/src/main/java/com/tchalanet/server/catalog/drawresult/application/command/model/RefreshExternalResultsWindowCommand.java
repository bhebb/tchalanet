package com.tchalanet.server.catalog.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record RefreshExternalResultsWindowCommand(
    TenantId tenantId, // null => global/platform mode
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys,
    boolean force,
    boolean dryRun,
    int maxSlots)
    implements Command<RefreshExternalResultsWindowResult> {}

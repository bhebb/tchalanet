package com.tchalanet.server.core.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record FetchExternalResultsWindowCommand(
    TenantId tenantId,
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys,
    boolean force,
    boolean dryRun,
    int maxSlots // safety cap (channels * dates)
    ) implements Command<FetchExternalResultsWindowResult> {}

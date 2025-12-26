package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateDrawsForRangeCommand(
    @NotNull TenantId tenantId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    boolean dryRun,
    boolean force)
    implements Command<GenerateDrawsForRangeResult> {}

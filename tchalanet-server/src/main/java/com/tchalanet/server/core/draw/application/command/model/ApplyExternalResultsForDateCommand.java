package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;
import java.util.List;

public record ApplyExternalResultsForDateCommand(
    TenantId tenantId, // option: null => tous tenants (platform)
    LocalDate drawDate,
    List<String> channelCodes,
    boolean force,
    boolean dryRun,
    int maxDraws)
    implements com.tchalanet.server.common.bus.Command<ApplyExternalResultsForDateResult> {}

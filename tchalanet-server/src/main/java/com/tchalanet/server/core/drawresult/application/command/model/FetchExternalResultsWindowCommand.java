package com.tchalanet.server.core.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;

public record FetchExternalResultsWindowCommand(
    TenantId tenantId,
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys,
    boolean force,
    boolean dryRun,
    int maxSlots, // safety cap (channels * dates)
    String reason // AJOUTÉ: obligatoire si force=true
    ) implements Command<FetchExternalResultsWindowResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

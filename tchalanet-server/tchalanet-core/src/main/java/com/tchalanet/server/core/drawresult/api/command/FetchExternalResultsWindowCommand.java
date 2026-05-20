package com.tchalanet.server.core.drawresult.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import java.util.List;

public record FetchExternalResultsWindowCommand(
    TenantId tenantId, // can be null (global fetch)
    LocalDate baseDate,
    int daysBack,
    List<String> slotKeys, // null or empty means all active slots
    boolean force,
    boolean dryRun,
    int maxSlots, // safety cap (channels * dates)
    String reason, // required if force=true
    boolean includeRaw // false by default
) implements Command<FetchExternalResultsWindowResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

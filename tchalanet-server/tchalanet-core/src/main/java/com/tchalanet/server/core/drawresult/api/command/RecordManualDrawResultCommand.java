package com.tchalanet.server.core.drawresult.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record RecordManualDrawResultCommand(
    TenantId tenantId,
    LocalDate drawDate,
    String slotKey, // ex: NY_MID
    String recordedBy,
    String notes,
    String pick3,
    String pick4,
    boolean force, // allows overwriting CONFIRMED/OVERRIDDEN
    String reason, // required when force=true
    boolean observeTrustPolicy // when true, status is driven by source_cfg.trust_policy
    ) implements Command<RecordManualDrawResultResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

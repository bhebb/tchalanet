package com.tchalanet.server.core.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.command.audit.AuditedForceCommand;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

@AuditedForceCommand
public record RecordManualDrawResultCommand(
    TenantId tenantId,
    LocalDate drawDate,
    String slotKey, // ✅ ex: NY_MID
    String recordedBy,
    String notes,
    String pick3,
    String pick4,
    boolean force, // ✅ même logique que override: permet écraser FINAL
    String reason // AJOUTÉ: obligatoire si force=true
    ) implements Command<RecordManualDrawResultResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

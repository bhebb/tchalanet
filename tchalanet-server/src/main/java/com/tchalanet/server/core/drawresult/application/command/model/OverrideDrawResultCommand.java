package com.tchalanet.server.core.drawresult.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.command.audit.AuditedForceCommand;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

@AuditedForceCommand
public record OverrideDrawResultCommand(
    TenantId tenantId,
    String slotKey, // ex: NY_MID
    LocalDate drawDate, // date du slot (locale slot)
    String pick3,
    String pick4,
    String reason,
    boolean force // true si tu veux écraser même FINAL (sinon bloque)
    ) implements Command<OverrideDrawResultResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

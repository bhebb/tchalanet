package com.tchalanet.server.core.draw.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Génère les tirages planifiés pour une plage de dates donnée.
 *
 * @param tenantId identifiant du tenant pour lequel générer les tirages
 * @param from     début de la plage (inclusif)
 * @param to       fin de la plage (inclusif) ; doit être ≥ {@code from}
 * @param dryRun   si {@code true}, simule sans aucune écriture en base
 * @param force    si {@code true}, re-génère même si les tirages existent déjà
 * @param reason   obligatoire si force=true
 */
public record GenerateDrawsForRangeCommand(
    @NotNull TenantId tenantId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    boolean dryRun,
    boolean force,
    String reason)
    implements Command<GenerateDrawsForRangeResult> {

  @AssertTrue(message = "reason is required when force is true")
  public boolean isReasonValidForForce() {
    return !force || (reason != null && !reason.isBlank());
  }
}

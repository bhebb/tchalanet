package com.tchalanet.server.draw.domain.usecase;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Récupère les informations du prochain tirage pour un tenant donné. */
public interface GetNextDrawForTenantUseCase {

  /**
   * @param tenantId tenant concerné (peut être null pour global)
   * @param now horodatage courant utilisé comme point de référence
   * @return map décrivant le prochain tirage (id, heure, compte-à-rebours, etc.)
   */
  Map<String, Object> getNextDraw(UUID tenantId, Instant now);
}

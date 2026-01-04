package com.tchalanet.server.core.sales.application.port.out;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;

public interface TicketSettlementQueryPort {

  /**
   * @return true s'il existe au moins un ticket encore "non-finalisé" pour ce draw
   */
  boolean existsPendingByDrawId(TenantId tenantId, DrawId drawId);

  /** Optionnel si tu veux de la télémétrie */
  long countPendingByDrawId(TenantId tenantId, DrawId drawId);
}

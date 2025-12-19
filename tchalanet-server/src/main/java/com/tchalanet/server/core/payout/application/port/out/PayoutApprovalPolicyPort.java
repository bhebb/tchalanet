package com.tchalanet.server.core.payout.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutApprovalPolicyPort {
  /** Retourne true si le payout peut être auto-approuvé (payé) immédiatement pour ce tenant/context. */
  boolean autoApprove(UUID tenantId, BigDecimal amount);
}


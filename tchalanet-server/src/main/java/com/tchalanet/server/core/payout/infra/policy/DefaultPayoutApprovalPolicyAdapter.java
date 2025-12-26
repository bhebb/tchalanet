package com.tchalanet.server.core.payout.infra.policy;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.payout.application.port.out.PayoutApprovalPolicyPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Default V1 policy: auto-approve when amount is below a hardcoded threshold (in cents). */
@Component
public class DefaultPayoutApprovalPolicyAdapter implements PayoutApprovalPolicyPort {

  // Fallback threshold: 50,000 HTG cents = 500.00 HTG (adjust to your needs)
  private static final long DEFAULT_THRESHOLD_CENTS = 50_000L;

  @Override
  public boolean autoApprove(TenantId tenantId, BigDecimal amount) {
    if (amount == null) return false;
    try {
      long cents = amount.movePointRight(2).longValue();
      return cents < DEFAULT_THRESHOLD_CENTS; // auto-approve if below threshold
    } catch (ArithmeticException ex) {
      return false;
    }
  }
}

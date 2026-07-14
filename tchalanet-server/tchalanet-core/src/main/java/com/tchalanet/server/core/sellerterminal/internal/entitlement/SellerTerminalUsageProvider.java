package com.tchalanet.server.core.sellerterminal.internal.entitlement;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalJpaRepository;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SellerTerminalUsageProvider implements UsageProvider {

  private final SellerTerminalJpaRepository repository;

  @Override
  public boolean supports(String usageKey) {
    return UsageKeys.SELLER_TERMINALS_ACTIVE.equals(usageKey);
  }

  @Override
  public int currentUsage(TenantId tenantId, String usageKey) {
    if (!supports(usageKey)) {
      throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
    }

    return Math.toIntExact(repository.countByTenantIdAndDeletedAtIsNull(tenantId.value()));
  }
}

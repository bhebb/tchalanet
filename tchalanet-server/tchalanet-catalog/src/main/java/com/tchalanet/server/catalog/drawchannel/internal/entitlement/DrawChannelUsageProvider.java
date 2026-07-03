package com.tchalanet.server.catalog.drawchannel.internal.entitlement;

import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelUsageProvider implements UsageProvider {

  private final DrawChannelRepository repository;

  @Override
  public boolean supports(String usageKey) {
    return UsageKeys.DRAW_CHANNELS_ACTIVE.equals(usageKey);
  }

  @Override
  public int currentUsage(TenantId tenantId, String usageKey) {
    if (!supports(usageKey)) {
      throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
    }

    return Math.toIntExact(repository.countByTenantIdAndActiveTrueAndDeletedAtIsNull(tenantId.value()));
  }
}

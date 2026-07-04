package com.tchalanet.server.platform.entitlement.internal.usage;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.entitlement.api.UsageKeys;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawChannelUsageProvider implements UsageProvider {

  private final DrawChannelCatalog drawChannelCatalog;

  @Override
  public boolean supports(String usageKey) {
    return UsageKeys.DRAW_CHANNELS_ACTIVE.equals(usageKey);
  }

  @Override
  public int currentUsage(TenantId tenantId, String usageKey) {
    if (!supports(usageKey)) {
      throw new IllegalArgumentException("Unsupported usageKey: " + usageKey);
    }

    return Math.toIntExact(drawChannelCatalog.countActiveChannels(tenantId));
  }
}

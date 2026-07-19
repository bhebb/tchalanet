package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.entitlement.api.UsageProvider;
import com.tchalanet.server.platform.entitlement.api.error.EntitlementErrorCodes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageResolver {

  private final List<UsageProvider> providers;

  public int currentUsage(TenantId tenantId, String usageKey) {
    var matching = providers.stream().filter(p -> p.supports(usageKey)).toList();

    if (matching.isEmpty()) {
      log.error(
          "entitlement.usage_provider.unavailable usageKey={} reason=none_registered", usageKey);
      throw ProblemRest.of(EntitlementErrorCodes.USAGE_PROVIDER_UNAVAILABLE);
    }

    if (matching.size() > 1) {
      log.error(
          "entitlement.usage_provider.unavailable usageKey={} reason=multiple_registered",
          usageKey);
      throw ProblemRest.of(EntitlementErrorCodes.USAGE_PROVIDER_UNAVAILABLE);
    }

    return matching.getFirst().currentUsage(tenantId, usageKey);
  }
}

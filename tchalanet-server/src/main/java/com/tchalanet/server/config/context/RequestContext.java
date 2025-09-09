package com.tchalanet.server.config.context;

import com.tchalanet.server.constants.TchRole;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// immuable = sûr & testable
public record RequestContext(
    String originalTenantId, // from JWT
    String tenantId, // effective (may be overridden by SA)
    String userId,
    Set<TchRole> systemRoles,
    Set<String> customRoles,
    Locale locale,
    String requestId, // corrélation logs
    String clientIp,
    boolean tenantOverridden // true if SA override applied
    ) {

  public UUID effectiveTenant() {
    var tenant = tenantOverridden ? tenantId : originalTenantId;
    return UUID.fromString(tenant);
  }
}

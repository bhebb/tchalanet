package com.tchalanet.server.common.context;

import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record TchRequestContext(
    String originalTenantCode, // from JWT claim (code like "demo")
    UUID originalTenantUuid, // resolved UUID (may be null)
    String effectiveTenantCode, // effective (may be overridden by SA)
    UUID effectiveTenantUuid, // resolved UUID (may be null)
    String userId,
    Set<TchRole> systemRoles,
    Set<String> customRoles,
    Locale locale,
    String requestId, // correlation id for logs
    String clientIp,
    boolean tenantOverridden // true if SA override applied
    ) {

  /** Return the effective tenant UUID when available, otherwise the original one. */
  public UUID tenantUuid() {
    return effectiveTenantUuid != null ? effectiveTenantUuid : originalTenantUuid;
  }

  public UUID userUuid() {

    try {
      if (userId != null) {
        return UUID.fromString(userId);
      }
    } catch (IllegalArgumentException exception) {
      log.error("Cannot convert string to UUID: '{}'", userId, exception);
    }
    return null;
  }

  public String userAgent() {
    // user agent get it
    return null;
  }
}

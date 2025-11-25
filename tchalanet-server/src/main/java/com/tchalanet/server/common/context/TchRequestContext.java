package com.tchalanet.server.common.context;

import com.tchalanet.server.common.domain.TchRole;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Immutable request-scoped context holding tenant/user info.
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

  /** Backward-compatible accessor used by existing code: string form of effective tenant id. */
  public String tenantId() {
    var t = tenantUuid();
    if (t != null) return t.toString();
    if (effectiveTenantCode != null && !effectiveTenantCode.isBlank()) return effectiveTenantCode;
    return originalTenantCode;
  }

  /** Backward-compatible accessor returning the original tenant id as String. */
  public String originalTenantId() {
    if (originalTenantUuid != null) return originalTenantUuid.toString();
    return originalTenantCode;
  }

  /** Backward-compatible method previously used to get a UUID for the effective tenant. */
  public UUID effectiveTenant() {
    var t = tenantUuid();
    if (t != null) return t;
    try {
      if (effectiveTenantCode != null && !effectiveTenantCode.isBlank()) {
        return UUID.fromString(effectiveTenantCode);
      }
      if (originalTenantCode != null && !originalTenantCode.isBlank()) {
        return UUID.fromString(originalTenantCode);
      }
    } catch (IllegalArgumentException ignored) {
      // not a UUID string; caller must handle null
    }
    return null;
  }
}

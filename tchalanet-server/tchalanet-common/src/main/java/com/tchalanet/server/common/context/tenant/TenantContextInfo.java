package com.tchalanet.server.common.context.tenant;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Minimal tenant defaults used by the execution context.
 * Keep this small: identity + formatting defaults (currency, timezone).
 */
public record TenantContextInfo(
    TenantId tenantId,
    String tenantCode,
    Currency currency,
    ZoneId tenantZoneId
) {
  public TenantContextInfo {
    if (tenantCode == null) throw new IllegalArgumentException("tenantCode is null");
    if (tenantId == null) throw new IllegalArgumentException("tenantId is null");
    if (currency == null) throw new IllegalArgumentException("currency is null");
    if (tenantZoneId == null) throw new IllegalArgumentException("tenantZoneId is null");
  }
}

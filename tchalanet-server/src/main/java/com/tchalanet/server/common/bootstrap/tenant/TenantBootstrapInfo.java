package com.tchalanet.server.common.bootstrap.tenant;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.ZoneId;
import java.util.Currency;

/**
 * Tenant bootstrap information used during initialization.
 * Contains essential tenant defaults: identity, code, timezone, and currency.
 */
public record TenantBootstrapInfo(
    String tenantCode,
    TenantId tenantId,
    ZoneId tenantZoneId,
    Currency currency
) {
  public TenantBootstrapInfo {
    if (tenantCode == null || tenantCode.isBlank()) throw new IllegalArgumentException("tenantCode is null or blank");
    if (tenantId == null) throw new IllegalArgumentException("tenantId is null");
    if (tenantZoneId == null) throw new IllegalArgumentException("tenantZoneId is null");
    if (currency == null) throw new IllegalArgumentException("currency is null");
  }
}

package com.tchalanet.server.platform.tenanttheme.api.model;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

/**
 * Notice emitted when theme preset fallback is applied.
 * Maps to spec requirement DP3 (NF1 - Observability).
 */
public record TenantThemeNotice(
    String code,
    TenantId tenantId,
    String requestedPresetCode,
    String fallbackPresetCode,
    Instant timestamp) {

  public static final String CODE_UNAVAILABLE_FALLBACK =
      "THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED";

  public static TenantThemeNotice fallbackApplied(
      TenantId tenantId, String requestedPresetCode, String fallbackPresetCode, Instant timestamp) {
    return new TenantThemeNotice(
        CODE_UNAVAILABLE_FALLBACK, tenantId, requestedPresetCode, fallbackPresetCode, timestamp);
  }
}

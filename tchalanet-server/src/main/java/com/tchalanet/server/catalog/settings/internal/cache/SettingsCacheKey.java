package com.tchalanet.server.catalog.settings.internal.cache;

import com.tchalanet.server.catalog.settings.api.model.ResolveSettingsCriteria;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;
import java.util.Objects;

/**
 * Settings Cache Key Generator (INTERNAL)
 *
 * <p>Generates deterministic cache keys for resolved settings queries.
 */
public final class SettingsCacheKey {

  private SettingsCacheKey() {}

  /**
   * Generate cache key from criteria.
   *
   * @param criteria resolution criteria
   * @return cache key
   */
  public static String of(ResolveSettingsCriteria criteria) {
    Objects.requireNonNull(criteria, "criteria is required");
    return of(
        criteria.tenantId(), criteria.outletId(), criteria.terminalId(), criteria.namespaces());
  }

  /**
   * Generate cache key for resolution query.
   *
   * <p>Format: "t=<tenantId>|o=<outletId>|m=<terminalId>|ns=<namespaces>"
   *
   * @param tenantId tenant ID (required)
   * @param outletId outlet ID (optional)
   * @param terminalId terminal ID (optional)
   * @param namespaces namespace filter (optional, normalized and sorted)
   * @return cache key
   */
  public static String of(
      TenantId tenantId, OutletId outletId, TerminalId terminalId, List<String> namespaces) {
    Objects.requireNonNull(tenantId, "tenantId is required");

    var ns = normalize(namespaces);
    return "t="
        + tenantId.value()
        + "|o="
        + (outletId == null ? "-" : outletId.value())
        + "|m="
        + (terminalId == null ? "-" : terminalId.value())
        + "|ns="
        + String.join(",", ns);
  }

  /**
   * Normalize namespace list for consistent cache keys.
   *
   * @param namespaces raw namespace list
   * @return normalized, sorted, deduplicated list
   */
  static List<String> normalize(List<String> namespaces) {
    if (namespaces == null) {
      return List.of();
    }
    return namespaces.stream()
        .filter(s -> s != null && !s.isBlank())
        .map(String::trim)
        .distinct()
        .sorted()
        .toList();
  }
}

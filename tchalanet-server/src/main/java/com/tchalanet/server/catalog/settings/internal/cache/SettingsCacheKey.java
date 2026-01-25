package com.tchalanet.server.catalog.settings.internal.cache;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Settings Cache Key Generator (INTERNAL)
 *
 * <p>Generates deterministic cache keys for resolved settings queries.
 */
public final class SettingsCacheKey {

  private SettingsCacheKey() {}

  /**
   * Generate cache key for resolution query.
   *
   * <p>Format: "t=<tenantId>|o=<outletId>|m=<terminalId>|ns=<namespaces>"
   *
   * @param tenantId tenant ID (required)
   * @param outletId outlet ID (optional)
   * @param terminalId terminal ID (optional)
   * @param namespaces namespace filter (optional, sorted)
   * @return cache key
   */
  public static String of(
      TenantId tenantId, OutletId outletId, TerminalId terminalId, List<String> namespaces) {
    Objects.requireNonNull(tenantId, "tenantId is required");

    List<String> ns = (namespaces == null) ? List.of() : namespaces;
    String nsKey =
        ns.stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .sorted()
            .collect(Collectors.joining(","));

    return "t="
        + tenantId.value()
        + "|o="
        + (outletId == null ? "-" : outletId.value())
        + "|m="
        + (terminalId == null ? "-" : terminalId.value())
        + "|ns="
        + nsKey;
  }
}

package com.tchalanet.server.common.batch;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.settings.AppSettingLevel;
import com.tchalanet.server.core.settings.infra.persistence.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * BatchGate contrôle si un job batch peut s'exécuter en lisant des app settings.
 *
 * <p>Utilise le CacheManager central (CombinedCacheManager) pour bénéficier du L1+Caffeine + L2
 * (Redis) et des TTL/strategies globales plutôt que d'avoir un cache local interne.
 */
@Service
@RequiredArgsConstructor
public class BatchGate {

  private final AppSettingRepository appSettingRepository;
  private final CacheManager cacheManager;

  private static final String CACHE_NAME = "batch_booleans";

  public boolean canRun(String jobKey) {
    if (!getBool("batch", "enabled", true)) return false;
    return getBool("batch", "jobs." + jobKey + ".enabled", true);
  }

  private boolean getBool(String ns, String key, boolean def) {
    String cacheKey = ns + "\u0000" + key;
    Cache c = cacheManager.getCache(CACHE_NAME);
    if (c == null) {
      // fallback: no cache configured -> load directly
      return loadBool(ns, key, def);
    }

    try {
      Boolean v = c.get(cacheKey, () -> Boolean.valueOf(loadBool(ns, key, def)));
      return v == null ? def : v.booleanValue();
    } catch (Exception ex) {
      // If cache loader throws, fallback to direct load
      return loadBool(ns, key, def);
    }
  }

  private boolean loadBool(String ns, String key, boolean def) {
    return appSettingRepository
        .findFirstByActiveTrueAndDeletedAtIsNullAndLevelAndTenantIdAndOutletIdAndTerminalIdAndNamespaceAndSettingKey(
            AppSettingLevel.GLOBAL, null, null, null, ns, key)
        .map(e -> parseBool(e.getSettingValue(), def))
        .orElse(def);
  }

  private static boolean parseBool(String v, boolean def) {
    if (v == null) return def;
    String t = v.trim();
    return "true".equalsIgnoreCase(t) || "1".equals(t);
  }

  public void assertCanRunOrThrow(String jobKey) {
    if (!canRun(jobKey)) {
      throw new BatchDisabledException(jobKey);
    }
  }

  /**
   * Generic accessor used by ops/controllers to read boolean flags under 'batch' namespace. Keep it
   * simple: default to true if absent.
   */
  public boolean enabled(String settingKey, TenantId tenantId) {
    // tenantId currently not used in this simple implementation; we keep a single global lookup
    return getBool("batch", settingKey, true);
  }
}

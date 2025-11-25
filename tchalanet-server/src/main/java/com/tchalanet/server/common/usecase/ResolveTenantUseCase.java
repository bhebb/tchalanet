package com.tchalanet.server.common.usecase;

import java.util.Optional;
import java.util.UUID;

public interface ResolveTenantUseCase {
  /**
   * Resolve a tenant code (e.g. "demo") to the UUID tenant id. Returns Optional.empty() when not
   * found.
   */
  Optional<UUID> resolveIdByCode(String tenantCode);

  /** Evict a tenant code from the cache. */
  void evictByCode(String tenantCode);

  /** Evict all cached tenant resolutions. */
  void evictAll();
}

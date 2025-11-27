package com.tchalanet.server.tenant.application.ports.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Inbound Port for resolving a tenant code (e.g. "demo") to the UUID tenant id. This is typically
 * used by internal services that need to work with tenant IDs.
 */
public interface ResolveTenantIdByCodeUseCase {
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

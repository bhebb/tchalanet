package com.tchalanet.server.core.tenant.infra.cache;

import com.tchalanet.server.common.types.id.TenantId;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Small cache helper for Tenant lookups. Follows the project guidelines: - constructor injection -
 * avoid returning nulls (use Optional) - keep a UUID-based compatibility surface
 */
@Component
public class TenantCache {

  private static final Logger log = LoggerFactory.getLogger(TenantCache.class);

  public static final String CACHE_TENANT_BY_CODE = "tenant_by_code";
  public static final String CACHE_TENANT_BY_ID = "tenant_by_id";

  private final CacheManager cacheManager;

  public TenantCache(CacheManager cacheManager) {
    this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager is required");
  }

  // --- New, modern API using TenantId record ---

  public Optional<TenantId> findTenantIdByCode(String codeLower) {
    Objects.requireNonNull(codeLower, "codeLower is required");
    String key = codeLower.trim().toLowerCase();
    Cache c = cacheManager.getCache(CACHE_TENANT_BY_CODE);
    if (c == null) {
      log.debug("Cache '{}' not configured", CACHE_TENANT_BY_CODE);
      return Optional.empty();
    }
    UUID id = c.get(key, UUID.class);
    return Optional.ofNullable(id).map(TenantId::of);
  }

  public void putTenantIdByCode(String codeLower, TenantId tenantId) {
    Objects.requireNonNull(codeLower, "codeLower is required");
    Objects.requireNonNull(tenantId, "tenantId is required");
    Cache c = cacheManager.getCache(CACHE_TENANT_BY_CODE);
    if (c == null) {
      log.debug(
          "Cache '{}' not configured, skipping put for slotKey={}",
          CACHE_TENANT_BY_CODE,
          codeLower);
      return;
    }
    c.put(codeLower.trim().toLowerCase(), tenantId.value());
  }

  public void evictByTenantId(TenantId tenantId) {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Cache c = cacheManager.getCache(CACHE_TENANT_BY_ID);
    if (c != null) c.evict(tenantId.value());
  }

  public void evictByCode(String codeLower) {
    Objects.requireNonNull(codeLower, "codeLower is required");
    Cache c = cacheManager.getCache(CACHE_TENANT_BY_CODE);
    if (c != null) c.evict(codeLower.trim().toLowerCase());
  }

  /** Evict after transaction commit (if inside transaction) otherwise evict immediately. */
  public void evictAfterCommit(TenantId tenantId, String codeLower) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              if (tenantId != null) evictByTenantId(tenantId);
              if (codeLower != null) evictByCode(codeLower);
            }
          });
    } else {
      if (tenantId != null) evictByTenantId(tenantId);
      if (codeLower != null) evictByCode(codeLower);
    }
  }
}

package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.TenantId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Use case pour invalider le cache du thème d'un tenant. Utilisé après publication ou modification
 * d'un thème.
 */
@Service
@RequiredArgsConstructor
public class EvictTenantThemeCacheUseCase {

  @CacheEvict(value = "publishedThemeByTenant", key = "#tenantId.value()")
  public void execute(TenantId tenantId) {
    // Cache eviction only - no-op
  }

  /** Variante acceptant directement un UUID pour compatibilité */
  @CacheEvict(value = "publishedThemeByTenant", key = "#tenantId")
  public void execute(UUID tenantId) {
    // Cache eviction only - no-op
  }
}

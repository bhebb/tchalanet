package com.tchalanet.server.tenant.domain.usecase;

import com.tchalanet.server.tenant.domain.model.TenantId;
import com.tchalanet.server.tenant.domain.model.ThemeMode;
import com.tchalanet.server.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.tenant.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.tenant.web.dto.ThemeRefDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Use case pour récupérer le thème publié d'un tenant. Simple use case de lecture avec cache. */
@Service
@RequiredArgsConstructor
public class GetPublishedThemeUseCase {

  private final JpaThemeRepository themeRepository;

  @Cacheable(
      value = "publishedThemeByTenant",
      key = "#tenantId.value()",
      unless = "#result == null")
  public ThemeRefDto execute(TenantId tenantId) {
    UUID uuid = tenantId.value();
    return themeRepository
        .findFirstByTenantIdAndStatusOrderByUpdatedAtDesc(uuid, ThemeStatus.PUBLISHED)
        .map(t -> new ThemeRefDto(t.getId().toString(), t.getMode(), t.getDensity()))
        .orElseGet(() -> new ThemeRefDto("tchalanet", ThemeMode.SYSTEM, (short) 0));
  }

  /** Variante acceptant directement un UUID pour compatibilité */
  @Cacheable(value = "publishedThemeByTenant", key = "#tenantId", unless = "#result == null")
  public ThemeRefDto execute(UUID tenantId) {
    return execute(new TenantId(tenantId));
  }
}

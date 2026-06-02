package com.tchalanet.server.platform.tenanttheme.internal.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.internal.service.TenantTheme;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for TenantTheme.
 * Maps to spec requirement T6.
 * RLS is enforced at DB level via policies.
 */
@Component
@RequiredArgsConstructor
public class TenantThemePersistenceAdapter {

  private final TenantThemeJpaRepository repository;

  public TenantTheme save(TenantTheme tenantTheme) {
    var entity = repository.findByTenantId(tenantTheme.tenantId().value())
        .orElse(new TenantThemeJpaEntity());
    entity.setTenantId(tenantTheme.tenantId().value());
    entity.setPresetCode(tenantTheme.presetCode());
    entity.setDefaultMode(tenantTheme.defaultMode() != null ? tenantTheme.defaultMode() : "SYSTEM");
    entity.setActive(tenantTheme.active());
    entity.setDefaultTheme(tenantTheme.isDefault());
    var saved = repository.save(entity);
    return toDomain(saved);
  }

  public void deactivate(TenantId tenantId) {
    repository.findByTenantId(tenantId.value()).ifPresent(e -> {
      e.setActive(false);
      repository.save(e);
    });
  }

  public Optional<TenantTheme> findByTenantId(TenantId tenantId) {
    return repository.findByTenantId(tenantId.value()).map(this::toDomain);
  }

  public Optional<TenantTheme> findActiveByTenantId(TenantId tenantId) {
    return repository.findByTenantIdAndActive(tenantId.value(), true).map(this::toDomain);
  }

  private TenantTheme toDomain(TenantThemeJpaEntity entity) {
    return new TenantTheme(
        TenantId.of(entity.getTenantId()),
        entity.getPresetCode(),
        entity.getDefaultMode() != null ? entity.getDefaultMode() : "SYSTEM",
        entity.isActive(),
        entity.isDefaultTheme(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : "system");
  }
}

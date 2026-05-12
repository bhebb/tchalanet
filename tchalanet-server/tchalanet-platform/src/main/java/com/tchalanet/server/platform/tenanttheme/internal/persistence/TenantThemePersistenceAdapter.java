package com.tchalanet.server.platform.tenanttheme.internal.persistence;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemePersistencePort;
import com.tchalanet.server.core.tenanttheme.application.port.out.TenantThemeReaderPort;
import com.tchalanet.server.core.tenanttheme.domain.model.TenantTheme;
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
public class TenantThemePersistenceAdapter
    implements TenantThemePersistencePort, TenantThemeReaderPort {

  private final TenantThemeJpaRepository repository;

  @Override
  public TenantTheme save(TenantTheme tenantTheme) {
    var entity =
        repository
            .findByTenantId(tenantTheme.tenantId().value())
            .orElse(new TenantThemeJpaEntity());

    entity.setTenantId(tenantTheme.tenantId().value());
    entity.setPresetCode(tenantTheme.presetCode());
    entity.setMetadata(tenantTheme.metadata());
    entity.setDefaultTheme(tenantTheme.isDefault());
    // Note: version and createdBy are managed by JPA (@Version) and AuditableEntity

    var saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public void deactivate(TenantId tenantId) {
    repository.deleteByTenantId(tenantId.value());
  }

  @Override
  public Optional<TenantTheme> findByTenantId(TenantId tenantId) {
    return repository.findByTenantId(tenantId.value()).map(this::toDomain);
  }

  private TenantTheme toDomain(TenantThemeJpaEntity entity) {
    return new TenantTheme(
        TenantId.of(entity.getTenantId()),
        entity.getPresetCode(),
        entity.getMetadata(),
        entity.isDefaultTheme(),
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : "system");
  }
}

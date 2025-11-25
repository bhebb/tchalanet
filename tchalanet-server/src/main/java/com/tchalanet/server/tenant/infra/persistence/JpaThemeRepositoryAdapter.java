package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.Theme;
import com.tchalanet.server.tenant.domain.model.ThemeStatus;
import com.tchalanet.server.tenant.domain.ports.ThemeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaThemeRepositoryAdapter implements ThemeRepository {

  private final JpaThemeRepository jpa;

  @Override
  public Optional<Theme> findById(UUID id) {
    return jpa.findById(id).map(this::toDomain);
  }

  @Override
  public List<Theme> findByTenantId(UUID tenantId) {
    return jpa.findByTenantId(tenantId).stream().map(this::toDomain).collect(Collectors.toList());
  }

  @Override
  public Optional<Theme> findFirstPublished(UUID tenantId) {
    return jpa.findFirstByTenantIdAndStatusOrderByUpdatedAtDesc(tenantId, ThemeStatus.PUBLISHED)
        .map(this::toDomain);
  }

  private Theme toDomain(ThemeJpaEntity e) {
    return new Theme(
        e.getId(),
        e.getTenantId(),
        e.getBasePresetId(),
        e.getLabel(),
        e.getMode(),
        e.getDensity(),
        e.getPalette(),
        e.getTokens(),
        e.getCssVars(),
        e.getStatus(),
        e.getThemeVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }
}

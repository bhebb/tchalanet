package com.tchalanet.server.core.theme.infra.persistence;

import com.tchalanet.server.core.theme.application.port.out.ThemeReaderPort;
import com.tchalanet.server.core.theme.application.port.out.ThemeWriterPort;
import com.tchalanet.server.core.theme.domain.model.Theme;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThemePersistenceAdapter implements ThemeReaderPort, ThemeWriterPort {

  private final JpaThemeRepository jpaThemeRepository;

  @Override
  public Optional<Theme> findById(UUID id) {
    return jpaThemeRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Theme> findPublishedById(UUID id) {
    return jpaThemeRepository.findById(id)
        .filter(e -> e.getStatus() == ThemeStatus.PUBLISHED)
        .map(this::toDomain);
  }

  @Override
  public List<Theme> listByTenantAndStatus(UUID tenantId, ThemeStatus status) {
    return jpaThemeRepository
        .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status)
        .stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Theme> findActiveForTenant(UUID tenantId) {
    return jpaThemeRepository
        .findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, ThemeStatus.PUBLISHED)
        .stream()
        .findFirst()
        .map(this::toDomain);
  }

  @Override
  @CacheEvict(cacheNames = "publishedThemeByTenant", key = "#theme.tenantId()")
  public Theme save(Theme theme) {
    ThemeJpaEntity entity = toEntity(theme);
    ThemeJpaEntity saved = jpaThemeRepository.save(entity);
    return toDomain(saved);
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

  private ThemeJpaEntity toEntity(Theme theme) {
    ThemeJpaEntity e = new ThemeJpaEntity();
    e.setId(theme.id());
    e.setTenantId(theme.tenantId());
    e.setBasePresetId(theme.basePresetId());
    e.setLabel(theme.label());
    e.setMode(theme.mode());
    e.setDensity(theme.density());
    e.setPalette(theme.palette());
    e.setTokens(theme.tokens());
    e.setCssVars(theme.cssVars());
    e.setStatus(theme.status());
    e.setThemeVersion(theme.themeVersion());
    return e;
  }
}


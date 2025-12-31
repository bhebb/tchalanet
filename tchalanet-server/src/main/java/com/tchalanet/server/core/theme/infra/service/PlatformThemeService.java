package com.tchalanet.server.core.theme.infra.service;

import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import com.tchalanet.server.core.theme.infra.persistence.JpaThemeRepository;
import com.tchalanet.server.core.theme.infra.persistence.ThemeJpaEntity;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformThemeService {

  private final JpaThemeRepository repo;

  public List<ThemeJpaEntity> list(ThemeStatus status) {
    if (status == null)
      return repo.findByStatusAndDeletedAtIsNull(
              ThemeStatus.PUBLISHED, org.springframework.data.domain.Pageable.unpaged())
          .getContent();
    return repo.findByStatusAndDeletedAtIsNull(
            status, org.springframework.data.domain.Pageable.unpaged())
        .getContent();
  }

  public ThemeJpaEntity get(UUID id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + id));
  }

  @Transactional
  public ThemeJpaEntity create(ThemeJpaEntity t) {
    t.setId(null);
    return repo.save(t);
  }

  @Transactional
  public ThemeJpaEntity update(UUID id, ThemeJpaEntity t) {
    var existing =
        repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Theme not found: " + id));
    existing.setBasePresetId(t.getBasePresetId());
    existing.setLabel(t.getLabel());
    existing.setMode(t.getMode());
    existing.setDensity(t.getDensity());
    existing.setPalette(t.getPalette());
    existing.setTokens(t.getTokens());
    existing.setCssVars(t.getCssVars());
    existing.setStatus(t.getStatus());
    existing.setThemeVersion(t.getThemeVersion());
    return repo.save(existing);
  }

  @Transactional
  public void delete(UUID id) {
    repo.deleteById(id);
  }
}

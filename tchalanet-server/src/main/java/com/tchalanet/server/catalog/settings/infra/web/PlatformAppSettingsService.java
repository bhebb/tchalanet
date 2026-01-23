package com.tchalanet.server.catalog.settings.infra.web;

import com.tchalanet.server.catalog.settings.infra.persistence.AppSettingEntity;
import com.tchalanet.server.catalog.settings.infra.persistence.AppSettingRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformAppSettingsService {

  private final AppSettingRepository repo;

  public List<AppSettingEntity> list() {
    // simple listing; can be refined by level/namespace
    return repo.findByActiveTrueAndDeletedAtIsNullAndLevelAndNamespaceIn(null, List.of());
  }

  public AppSettingEntity get(UUID id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("AppSetting not found: " + id));
  }

  @Transactional
  public AppSettingEntity create(AppSettingEntity e) {
    e.setId(null);
    return repo.save(e);
  }

  @Transactional
  public AppSettingEntity update(UUID id, AppSettingEntity e) {
    var existing =
        repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AppSetting not found: " + id));
    existing.setLevel(e.getLevel());
    existing.setNamespace(e.getNamespace());
    existing.setSettingKey(e.getSettingKey());
    existing.setSettingValue(e.getSettingValue());
    existing.setActive(e.getActive());
    return repo.save(existing);
  }

  @Transactional
  public void delete(UUID id) {
    repo.deleteById(id);
  }
}

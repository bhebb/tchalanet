package com.tchalanet.server.core.settings;

import com.tchalanet.server.common.config.SpringContextHolder;
import com.tchalanet.server.core.settings.infra.persistence.AppSettingEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppSettingCacheEvictListener {

  @PostPersist
  @PostUpdate
  @PostRemove
  public void onChange(AppSettingEntity entity) {
    try {
      var cache = SpringContextHolder.getBean(org.springframework.cache.CacheManager.class);
      var c = cache.getCache("app_settings_resolved");
      if (c != null) c.clear(); // v1
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}

package com.tchalanet.server.catalog.game.infra.cache;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameCacheSweepJob {

  // Si tu as 2 cache managers séparés (ex: caffeine + redis)
  private final List<CacheManager> cacheManagers;

  private static final String[] GAME_CACHES = {"game.byId", "game.byCode", "game.active"};

  // Tous les jours à 04:00 (America/Montreal)
  @Scheduled(cron = "0 0 4 * * *", zone = "America/Montreal")
  public void clearGameCachesDaily() {
    if (cacheManagers == null || cacheManagers.isEmpty()) {
      log.warn("No CacheManager configured for GameCacheSweepJob");
      return;
    }

    for (CacheManager cm : cacheManagers) {
      if (cm == null) continue;
      for (String name : GAME_CACHES) {
        var cache = cm.getCache(name);
        if (cache != null) {
          try {
            cache.clear();
          } catch (Exception e) {
            log.warn(
                "Failed to clear cache {} on manager {}: {}", name, cm.getClass(), e.toString());
          }
        }
      }
    }

    log.info("Game caches cleared: {}", (Object) GAME_CACHES);
  }
}

package com.tchalanet.server.catalog.game.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Games are a platform referential, edited very rarely and evicted on write. Tier A: 30 min / 12 h. */
@Component
public class GameCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofMinutes(30);
  private static final Duration L2 = Duration.ofHours(12);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(GameCacheNames.ACTIVE_GAMES, L1, L2),
        CacheSpec.of(GameCacheNames.GAME_BY_CODE, L1, L2),
        CacheSpec.of(GameCacheNames.GAME_BY_ID, L1, L2));
  }
}

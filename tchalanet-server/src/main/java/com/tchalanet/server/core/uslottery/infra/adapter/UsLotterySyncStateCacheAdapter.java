package com.tchalanet.server.core.uslottery.infra.adapter;

import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.domain.ports.out.UsLotterySyncStatePort;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Cache-based implementation using Spring CacheManager (Caffeine + Redis combined if configured).
 * Stores expiry epoch seconds as cache value, checks expiry on read. Used when no low-level
 * StringRedisTemplate is available.
 */
@Component
@ConditionalOnBean(CacheManager.class)
@ConditionalOnMissingBean(UsLotterySyncStateRedisAdapter.class)
public class UsLotterySyncStateCacheAdapter implements UsLotterySyncStatePort {

  private final CacheManager cacheManager;
  private final UsLotteryProperties props;
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final String CACHE_NAME = "uslottery.sync";

  public UsLotterySyncStateCacheAdapter(CacheManager cacheManager, UsLotteryProperties props) {
    this.cacheManager = cacheManager;
    this.props = props;
  }

  private String keyFor(LatestDraw d) {
    var date = d.drawTimeUtc().toLocalDate();
    return String.format("uslottery:sync:%s:%s:%s", d.provider().name(), d.channelCode(), DATE_FMT.format(date));
  }

  @Override
  public boolean shouldFetch(LatestDraw probe) {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) return true;
    String key = keyFor(probe);
    Cache.ValueWrapper wrapper = cache.get(key);
    if (wrapper == null) return true;
    Object v = wrapper.get();
    if (v instanceof Long expiry) {
      long now = Instant.now().getEpochSecond();
      if (expiry <= now) {
        cache.evict(key);
        return true;
      }
      return false;
    }
    // unknown value, evict and allow fetch
    cache.evict(key);
    return true;
  }

  @Override
  public void markFetchAttempt(LatestDraw probe) {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) return;
    String key = keyFor(probe);
    long ttl = props.getSyncTtlSeconds();
    long expiry = Instant.now().getEpochSecond() + ttl;
    cache.put(key, expiry);
  }
}

package com.tchalanet.server.core.drawresult.internal.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Draw results are settlement-adjacent. A published result is immutable and every write path
 * (manual upsert, confirm, override) evicts these caches after commit, so the TTL is only a tight
 * safety net — kept short on purpose.
 */
@Component
public class DrawResultCacheSpecProvider implements CacheSpecProvider {

  private static final Duration L1 = Duration.ofSeconds(30);
  private static final Duration L2 = Duration.ofMinutes(2);

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(DrawResultCacheNames.BY_ID, L1, L2),
        CacheSpec.of(DrawResultCacheNames.ID_BY_SLOT_OCCURRED, L1, L2));
  }
}

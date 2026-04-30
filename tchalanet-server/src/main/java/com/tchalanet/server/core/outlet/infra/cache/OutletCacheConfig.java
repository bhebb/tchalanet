package com.tchalanet.server.core.outlet.infra.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OutletCacheConfig implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        // Infos outlet : 10–30 min, v1 = 20 min
        CacheSpec.of("tenant_outlet", Duration.ofMinutes(20)),
        // Config terminal : idem
        CacheSpec.of("tenant_terminal", Duration.ofMinutes(20)),
        // Arbre complet outlets/terminaux : 5–15 min, v1 = 10 min
        CacheSpec.of("tenant_outlet_tree", Duration.ofMinutes(10)));
  }
}

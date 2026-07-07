package com.tchalanet.server.catalog.i18n.internal.cache;

import com.tchalanet.server.common.cache.CacheSpec;
import com.tchalanet.server.common.cache.CacheSpecProvider;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Resolved i18n overrides per tenant/locale, changed rarely and evicted on write. Tier B: 15 min / 6 h. */
@Component
public class I18nOverridesCacheSpecProvider implements CacheSpecProvider {

  @Override
  public List<CacheSpec> cacheSpecs() {
    return List.of(
        CacheSpec.of(
            I18nOverridesCacheNames.RESOLVED_BY_LOCALE, Duration.ofMinutes(15), Duration.ofHours(6)));
  }
}

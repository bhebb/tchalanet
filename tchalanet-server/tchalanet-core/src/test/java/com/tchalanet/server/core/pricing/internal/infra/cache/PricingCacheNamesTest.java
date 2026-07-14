package com.tchalanet.server.core.pricing.internal.infra.cache;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PricingCacheNamesTest {

  @Test
  void constantsAreDefined() {
    assertNotNull(PricingCacheNames.TENANT_ODDS_LIST);
    assertNotNull(PricingCacheNames.TENANT_ODDS_BY_VARIANT);
  }
}

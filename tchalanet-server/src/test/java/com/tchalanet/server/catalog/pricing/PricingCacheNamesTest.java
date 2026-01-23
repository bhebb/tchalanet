package com.tchalanet.server.catalog.pricing;

import com.tchalanet.server.catalog.pricing.internal.cache.PricingCacheNames;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PricingCacheNamesTest {

  @Test
  void constants_are_defined() {
    assertNotNull(PricingCacheNames.ODDS);
  }
}

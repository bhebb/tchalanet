package com.tchalanet.server.core.pricing.internal.infra.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PricingCacheNamesTest {

    @Test
    void constantsAreDefined() {
        assertNotNull(PricingCacheNames.TENANT_ODDS_LIST);
        assertNotNull(PricingCacheNames.TENANT_ODDS_BY_VARIANT);
    }
}

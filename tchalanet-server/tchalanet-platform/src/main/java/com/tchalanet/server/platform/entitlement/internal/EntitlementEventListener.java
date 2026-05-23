package com.tchalanet.server.platform.entitlement.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntitlementEventListener {

    @EventListener
    @CacheEvict(cacheNames = "platform.entitlement.tenant_snapshot", key = "#event.tenantId()")
    public void onSubscriptionUpdated(com.tchalanet.server.platform.entitlement.api.event.TenantSubscriptionUpdatedEvent event) {
        log.info("Evicting entitlement snapshot for tenant {} due to subscription update", event.tenantId());
    }

    @EventListener
    @CacheEvict(cacheNames = "platform.entitlement.tenant_snapshot", key = "#event.tenantId()")
    public void onSubscriptionCanceled(com.tchalanet.server.platform.entitlement.api.event.TenantSubscriptionCanceledEvent event) {
        log.info("Evicting entitlement snapshot for tenant {} due to subscription cancellation", event.tenantId());
    }
}

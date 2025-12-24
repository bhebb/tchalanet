package com.tchalanet.server.core.billing.application.port.out;

import com.tchalanet.server.core.billing.domain.model.Subscription;
import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionReaderPort {
    Optional<Subscription> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(UUID tenantId, List<SubscriptionStatus> statuses);
    Optional<Subscription> findFirstByTenantIdAndStatus(UUID tenantId, Set<SubscriptionStatus> statuses);
    List<Subscription> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, Instant before);
}

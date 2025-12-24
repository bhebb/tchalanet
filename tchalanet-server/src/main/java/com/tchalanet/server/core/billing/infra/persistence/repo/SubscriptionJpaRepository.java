package com.tchalanet.server.core.billing.infra.persistence.repo;

import com.tchalanet.server.core.billing.domain.model.SubscriptionStatus;
import com.tchalanet.server.core.billing.infra.persistence.SubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
    Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(UUID tenantId, List<SubscriptionStatus> statuses);
    List<SubscriptionJpaEntity> findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus status, Instant before);
}

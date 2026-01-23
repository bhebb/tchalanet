package com.tchalanet.server.catalog.billing.infra.persistence.repo;

import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import com.tchalanet.server.catalog.billing.infra.persistence.SubscriptionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
  Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      UUID tenantId, List<SubscriptionStatus> statuses);

  List<SubscriptionJpaEntity> findByStatusAndCurrentPeriodEndBefore(
      SubscriptionStatus status, Instant before);
}

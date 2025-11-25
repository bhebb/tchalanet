package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.SubscriptionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
  Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      UUID tenantId, Collection<SubscriptionStatus> statuses);

  List<SubscriptionJpaEntity> findByTenantId(UUID tenantId);

  boolean existsByPlanId(UUID planId);

  List<SubscriptionJpaEntity> findByStatusAndCurrentPeriodEndBefore(
      SubscriptionStatus status, Instant before);
}

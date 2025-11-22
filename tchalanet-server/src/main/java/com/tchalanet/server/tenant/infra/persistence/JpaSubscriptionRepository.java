package com.tchalanet.server.tenant.infra.persistence;

import com.tchalanet.server.tenant.domain.model.SubscriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
  Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      String tenantId, Collection<SubscriptionStatus> statuses);

  List<SubscriptionJpaEntity> findByTenantId(String tenantId);
}

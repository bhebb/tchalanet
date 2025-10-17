package com.tchalanet.server.repository;

import com.tchalanet.server.model.Subscription;
import com.tchalanet.server.model.SubscriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
  Optional<Subscription> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      String tenantId, Collection<SubscriptionStatus> statuses);

  List<Subscription> findByTenantId(String tenantId);
}

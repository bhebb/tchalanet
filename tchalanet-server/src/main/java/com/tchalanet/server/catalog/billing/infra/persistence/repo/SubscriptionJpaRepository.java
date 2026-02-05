package com.tchalanet.server.catalog.billing.infra.persistence.repo;

import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import com.tchalanet.server.catalog.billing.infra.persistence.SubscriptionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
  Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      UUID tenantId, List<SubscriptionStatus> statuses);

  List<SubscriptionJpaEntity> findByStatusAndCurrentPeriodEndBefore(
      SubscriptionStatus status, Instant before);

  long countByDeletedAtIsNull();

  long countByStatusAndDeletedAtIsNull(SubscriptionStatus status);

  @Query(
      "SELECT p.code, COUNT(s), "
          + "SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) "
          + "FROM SubscriptionJpaEntity s JOIN s.plan p "
          + "WHERE s.deletedAt IS NULL "
          + "GROUP BY p.code")
  List<Object[]> countByPlanGrouped();
}

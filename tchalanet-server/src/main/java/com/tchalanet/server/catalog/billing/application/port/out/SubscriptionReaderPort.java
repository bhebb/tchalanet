package com.tchalanet.server.catalog.billing.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.catalog.billing.domain.model.Subscription;
import com.tchalanet.server.catalog.billing.domain.model.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionReaderPort {
  Optional<Subscription> findFirstByTenantIdAndStatusInOrderByCurrentPeriodStartDesc(
      TenantId tenantId, List<SubscriptionStatus> statuses);

  Optional<Subscription> findFirstByTenantIdAndStatus(
      TenantId tenantId, Set<SubscriptionStatus> statuses);

  List<Subscription> findByStatusAndCurrentPeriodEndBefore(
      SubscriptionStatus status, Instant before);
}

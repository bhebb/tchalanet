package com.tchalanet.server.core.tenant.domain.ports;

import com.tchalanet.server.core.tenant.domain.model.Subscription;
import com.tchalanet.server.core.tenant.domain.model.SubscriptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
  Optional<Subscription> findById(UUID id);

  Optional<Subscription> findFirstActiveByTenant(
      UUID tenantId, Collection<SubscriptionStatus> statuses);

  List<Subscription> findByTenantId(UUID tenantId);

  Subscription save(Subscription subscription);
}

package com.tchalanet.server.core.subscription.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.subscription.domain.model.Subscription;

import java.util.Optional;

/**
 * Port for reading subscription data (read-only).
 * Maps to spec requirement S2 (resolve tenant subscription).
 */
public interface SubscriptionReaderPort {

  /**
   * Find subscription by tenant ID.
   * Returns empty if no subscription exists for tenant.
   */
  Optional<Subscription> findByTenantId(TenantId tenantId);
}

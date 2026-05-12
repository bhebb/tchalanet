package com.tchalanet.server.core.subscription.internal.application.port.out;

import com.tchalanet.server.core.subscription.internal.domain.model.Subscription;

/**
 * Port for writing subscription data (persistence).
 * Maps to spec requirement S1 (persist subscription state).
 */
public interface SubscriptionPersistencePort {

  /**
   * Save or update subscription.
   * Increments version (optimistic locking per S4).
   */
  Subscription save(Subscription subscription);
}

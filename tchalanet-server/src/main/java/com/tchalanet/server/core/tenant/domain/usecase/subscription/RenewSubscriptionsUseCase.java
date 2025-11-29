package com.tchalanet.server.core.tenant.domain.usecase.subscription;

public interface RenewSubscriptionsUseCase {
  /** Renew due subscriptions - V1 can be a fake provider implementation. */
  void renewDueSubscriptions();
}

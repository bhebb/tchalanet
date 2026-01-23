package com.tchalanet.server.catalog.billing.domain.model;

public enum SubscriptionStatus {
  ACTIVE,
  TRIALING,
  CANCELED,
  PAST_DUE,
  SUSPENDED;

  public boolean isCancelable() {
    return this == TRIALING || this == ACTIVE || this == SUSPENDED;
  }
}

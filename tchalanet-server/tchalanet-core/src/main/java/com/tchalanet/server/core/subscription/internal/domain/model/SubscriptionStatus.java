package com.tchalanet.server.core.subscription.internal.domain.model;

/**
 * Subscription status enum (tenant lifecycle).
 * Maps to spec requirement S3 (lifecycle transitions).
 * Minimum MVP states per spec.
 */
public enum SubscriptionStatus {
  TRIAL,
  ACTIVE,
  SUSPENDED,
  CANCELED,
  EXPIRED
}

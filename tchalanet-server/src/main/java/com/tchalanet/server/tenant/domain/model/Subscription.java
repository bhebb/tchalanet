package com.tchalanet.server.tenant.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Subscription(
    UUID id,
    UUID tenantId,
    UUID planId,
    SubscriptionStatus status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    BillingProvider billingProvider,
    String billingExternalId,
    Map<String, Object> meta,
    long version) {}

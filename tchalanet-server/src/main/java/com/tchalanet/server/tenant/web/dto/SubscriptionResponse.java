package com.tchalanet.server.tenant.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
    UUID id,
    String tenantId,
    PlanResponse plan,
    String status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    String provider,
    String providerSubId) {}

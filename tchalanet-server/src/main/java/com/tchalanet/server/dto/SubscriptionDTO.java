package com.tchalanet.server.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionDTO(
    UUID id,
    String tenantId,
    PlanDTO plan,
    String status,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    boolean cancelAtPeriodEnd,
    String provider,
    String providerSubId) {}

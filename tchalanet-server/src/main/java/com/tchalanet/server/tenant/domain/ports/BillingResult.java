package com.tchalanet.server.tenant.domain.ports;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BillingResult(
    boolean success,
    UUID externalSubscriptionId,
    Instant periodStart,
    Instant periodEnd,
    Map<String, Object> meta,
    String message) {}

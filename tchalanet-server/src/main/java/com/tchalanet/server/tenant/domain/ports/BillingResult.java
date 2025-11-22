package com.tchalanet.server.tenant.domain.ports;

import java.time.Instant;
import java.util.Map;

public record BillingResult(
    boolean success,
    String externalSubscriptionId,
    Instant periodStart,
    Instant periodEnd,
    Map<String, Object> meta,
    String message) {}

package com.tchalanet.server.port;

import java.time.Instant;
import java.util.Map;

public record BillingResult(
    boolean success,
    String externalSubscriptionId,
    Instant periodStart,
    Instant periodEnd,
    Map<String, Object> meta,
    String message) {}

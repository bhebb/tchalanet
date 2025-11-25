package com.tchalanet.server.draw.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LimitPolicy(
    UUID id, UUID tenantId, String scope, String target, BigDecimal dailyCap, String onBreach) {}

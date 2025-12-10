package com.tchalanet.server.core.limitpolicy.infra.web.dto;

import com.tchalanet.server.core.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LimitPolicyResponse(
    UUID id,
    UUID tenantId,
    LimitScope scope,
    String target,
    BigDecimal dailyCap,
    BigDecimal maxStakePerLine,
    BigDecimal maxPayoutPerLine,
    BreachOutcome onBreach,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}

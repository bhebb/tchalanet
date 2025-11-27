package com.tchalanet.server.limitpolicy.web.dto;

import com.tchalanet.server.limitpolicy.domain.model.BreachOutcome;
import com.tchalanet.server.limitpolicy.domain.model.LimitScope;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record LimitPolicyRequest(
    UUID id, // Null for creation, UUID for update
    @NotNull LimitScope scope,
    String target, // e.g., gameCode, terminalId, userId
    BigDecimal dailyCap,
    BigDecimal maxStakePerLine,
    BigDecimal maxPayoutPerLine,
    @NotNull BreachOutcome onBreach,
    boolean active) {}

package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.core.limitpolicy.domain.model.ValidationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingValidationDto(
    UUID id,
    ValidationType type,
    String target,
    BigDecimal requestedAmount,
    UUID requestedBy,
    Instant requestedAt
) {}


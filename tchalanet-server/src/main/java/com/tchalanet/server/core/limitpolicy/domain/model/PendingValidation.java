package com.tchalanet.server.core.limitpolicy.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PendingValidation {
    UUID id;
    UUID tenantId;
    com.tchalanet.server.core.limitpolicy.domain.model.ValidationType type;
    String target;
    BigDecimal requestedAmount;
    UUID requestedBy;
    Instant requestedAt;
    com.tchalanet.server.core.limitpolicy.domain.model.ValidationStatus status;

    public PendingValidation(UUID id, UUID tenantId, com.tchalanet.server.core.limitpolicy.domain.model.ValidationType type, String target, BigDecimal requestedAmount, UUID requestedBy, Instant requestedAt, com.tchalanet.server.core.limitpolicy.domain.model.ValidationStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.type = type;
        this.target = target;
        this.requestedAmount = requestedAmount;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = status;
    }

    // getters
    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public com.tchalanet.server.core.limitpolicy.domain.model.ValidationType type() { return type; }
    public String target() { return target; }
    public BigDecimal requestedAmount() { return requestedAmount; }
    public UUID requestedBy() { return requestedBy; }
    public Instant requestedAt() { return requestedAt; }
    public com.tchalanet.server.core.limitpolicy.domain.model.ValidationStatus status() { return status; }
}

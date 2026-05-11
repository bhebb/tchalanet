package com.tchalanet.server.core.payout.infra.web.model;

import com.tchalanet.server.common.types.id.PayoutId;

import java.time.Instant;

public record PayoutWorkflowResponse(
    PayoutId payoutId,
    String status,
    Instant occurredAt) {
}

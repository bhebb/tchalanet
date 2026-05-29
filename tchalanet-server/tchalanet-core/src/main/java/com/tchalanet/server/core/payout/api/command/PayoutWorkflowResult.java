package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
import java.time.Instant;

public record PayoutWorkflowResult(PayoutId payoutId, PayoutClaimStatus status, Instant occurredAt) {}

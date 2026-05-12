package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;
import java.time.Instant;

public record PayoutWorkflowResult(PayoutId payoutId, PayoutStatus status, Instant occurredAt) {}

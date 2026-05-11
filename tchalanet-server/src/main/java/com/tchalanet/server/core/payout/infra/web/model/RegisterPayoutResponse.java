package com.tchalanet.server.core.payout.infra.web.model;

import com.tchalanet.server.common.types.id.PayoutId;

import java.math.BigDecimal;
import java.util.List;

public record RegisterPayoutResponse(
    PayoutId payoutId,
    String status,
    String payoutStatus,
    BigDecimal amount,
    String currency,
    List<String> warnings) {}

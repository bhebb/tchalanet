package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.domain.model.PayoutStatus;

import java.math.BigDecimal;
import java.util.List;

public enum RegisterPayoutStatus {
    PAID,
    REQUESTED,
    BLOCKED;
}

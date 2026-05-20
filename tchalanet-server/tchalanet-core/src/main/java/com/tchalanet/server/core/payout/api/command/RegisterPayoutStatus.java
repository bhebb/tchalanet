package com.tchalanet.server.core.payout.api.command;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;

import java.math.BigDecimal;
import java.util.List;

public enum RegisterPayoutStatus {
    PAID,
    REQUESTED,
    BLOCKED;
}

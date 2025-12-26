package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.sales.application.command.model.LimitNotice;
import java.util.List;
import java.util.UUID;

public record RegisterPayoutResult(
    Payout payout,
    String status, // "SUCCESS", "PENDING_APPROVAL"
    List<LimitNotice> warnings,
    UUID approvalRequestId) {}

package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.payout.domain.model.Payout;

public record ExecutePayoutCommand(TenantId tenantId, PayoutId payoutId, UserId executedBy)
    implements Command<Payout> {}

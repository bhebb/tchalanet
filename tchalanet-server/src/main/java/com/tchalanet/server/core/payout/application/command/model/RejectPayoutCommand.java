package com.tchalanet.server.core.payout.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.time.Instant;

public record RejectPayoutCommand(
    TenantId tenantId, PayoutId payoutId, String reason, UserId rejectedBy, Instant rejectedAt)
    implements Command<Void> {}

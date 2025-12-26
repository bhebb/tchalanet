package com.tchalanet.server.core.ledger.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record ReconcileDailyLedgerCommand(TenantId tenantId, Instant dayStart, Instant dayEnd)
    implements Command<Void> {}

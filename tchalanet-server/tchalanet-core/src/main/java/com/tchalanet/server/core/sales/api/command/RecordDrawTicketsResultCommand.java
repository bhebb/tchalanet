package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.TenantId;
import java.time.Instant;

public record RecordDrawTicketsResultCommand(
    DrawId drawId,
    TenantId tenantId,
    DrawResultId drawResultId,
    Instant occurredAt)
    implements Command<com.tchalanet.server.core.sales.api.command.RecordDrawTicketsResultResult> {

  public RecordDrawTicketsResultCommand(DrawId drawId) {
    this(drawId, null, null, null);
  }
}

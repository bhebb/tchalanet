package com.tchalanet.server.core.drawresult.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.drawresult.api.model.ResultReminderReason;
import java.time.Instant;
import java.time.LocalDate;

public record CreateMissingResultReminderCommand(
    ResultSlotId resultSlotId,
    LocalDate drawDate,
    Instant occurredAt,
    ResultReminderReason reason,
    String providerCode,
    String slotKey,
    String requestId)
    implements Command<CreateMissingResultReminderResult> {}

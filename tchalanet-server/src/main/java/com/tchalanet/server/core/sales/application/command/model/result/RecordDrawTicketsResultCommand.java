package com.tchalanet.server.core.sales.application.command.model.result;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;

public record RecordDrawTicketsResultCommand(DrawId drawId)
    implements Command<com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultResult> {}


package com.tchalanet.server.core.sales.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;

public record RecordDrawTicketsResultCommand(DrawId drawId)
    implements Command<com.tchalanet.server.core.sales.application.command.model.RecordDrawTicketsResultResult> {}


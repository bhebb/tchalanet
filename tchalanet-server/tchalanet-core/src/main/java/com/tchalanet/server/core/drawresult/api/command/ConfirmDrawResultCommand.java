package com.tchalanet.server.core.drawresult.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawResultId;

public record ConfirmDrawResultCommand(
    DrawResultId drawResultId,
    String confirmedBy
) implements Command<ConfirmDrawResultResult> {}

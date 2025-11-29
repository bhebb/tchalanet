package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.ResetSettlementForDrawCommand;

public interface ResetSettlementForDrawCommandHandler {
  void handle(ResetSettlementForDrawCommand command);
}

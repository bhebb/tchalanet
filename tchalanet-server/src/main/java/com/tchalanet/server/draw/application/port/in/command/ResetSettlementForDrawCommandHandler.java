package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.ResetSettlementForDrawCommand;

public interface ResetSettlementForDrawCommandHandler {
  void handle(ResetSettlementForDrawCommand command);
}

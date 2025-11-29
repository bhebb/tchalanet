package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.RetrySettleDrawCommand;

public interface RetrySettleDrawCommandHandler {
  void handle(RetrySettleDrawCommand command);
}

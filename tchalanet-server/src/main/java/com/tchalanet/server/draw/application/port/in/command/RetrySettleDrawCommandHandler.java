package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.RetrySettleDrawCommand;

public interface RetrySettleDrawCommandHandler {
  void handle(RetrySettleDrawCommand command);
}

package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.UpdateDrawCommand;

public interface UpdateDrawCommandHandler {

  void handle(UpdateDrawCommand command);
}

package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.UpdateDrawCommand;

public interface UpdateDrawCommandHandler {

  void handle(UpdateDrawCommand command);
}

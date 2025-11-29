package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.CreateDrawCommand;
import java.util.UUID;

public interface CreateDrawCommandHandler {
  UUID handle(CreateDrawCommand command);
}

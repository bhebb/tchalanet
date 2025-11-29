package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.UpdateDrawChannelCommand;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public interface UpdateDrawChannelCommandHandler {
  DrawChannel handle(UpdateDrawChannelCommand command);
}

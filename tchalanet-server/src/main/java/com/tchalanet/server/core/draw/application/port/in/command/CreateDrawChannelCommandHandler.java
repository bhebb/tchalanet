package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public interface CreateDrawChannelCommandHandler {
  DrawChannel handle(CreateDrawChannelCommand command);
}

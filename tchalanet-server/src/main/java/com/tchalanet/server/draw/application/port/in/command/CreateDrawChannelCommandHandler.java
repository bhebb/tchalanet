package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.CreateDrawChannelCommand;
import com.tchalanet.server.draw.domain.model.DrawChannel;

public interface CreateDrawChannelCommandHandler {
  DrawChannel handle(CreateDrawChannelCommand command);
}

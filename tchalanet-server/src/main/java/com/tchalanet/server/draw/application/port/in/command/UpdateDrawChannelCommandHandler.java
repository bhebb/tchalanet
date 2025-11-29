package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.application.command.model.UpdateDrawChannelCommand;
import com.tchalanet.server.draw.domain.model.DrawChannel;

public interface UpdateDrawChannelCommandHandler {
  DrawChannel handle(UpdateDrawChannelCommand command);
}

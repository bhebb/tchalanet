package com.tchalanet.server.core.draw.application.port.in.command;

import com.tchalanet.server.core.draw.domain.model.DrawChannel;

public interface ArchiveDrawChannelCommandHandler {
  void handle(DrawChannel channel);
}

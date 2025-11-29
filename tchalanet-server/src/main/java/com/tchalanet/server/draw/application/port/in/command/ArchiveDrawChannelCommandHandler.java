package com.tchalanet.server.draw.application.port.in.command;

import com.tchalanet.server.draw.domain.model.DrawChannel;

public interface ArchiveDrawChannelCommandHandler {
  void handle(DrawChannel channel);
}

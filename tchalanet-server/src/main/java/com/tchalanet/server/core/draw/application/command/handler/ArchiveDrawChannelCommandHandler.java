package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ArchiveDrawChannelCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawChannelId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveDrawChannelCommandHandler
    implements VoidCommandHandler<ArchiveDrawChannelCommand> {

  private final DrawChannelWriterPort drawChannelWriterPort;

  @Override
  public void handle(ArchiveDrawChannelCommand command) {
    log.info(
        "ArchiveDrawChannelCommandHandler.handle - archiving channel id={}", command.channelId());
    // ici on utilise deleteById pour supprimer/archiver le channel via le port
    drawChannelWriterPort.deleteById(new DrawChannelId(command.channelId()));
  }
}

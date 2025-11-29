package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.port.in.command.ArchiveDrawChannelCommandHandler;
import com.tchalanet.server.core.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.core.draw.domain.model.DrawChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveDrawChannelUseCase implements ArchiveDrawChannelCommandHandler {

  private final DrawChannelWriterPort drawChannelWriterPort;

  @Override
  public void handle(DrawChannel channel) {
    log.info("ArchiveDrawChannelUseCase.handle - archiving channel id={}", channel.id());
    // ici on utilise deleteById pour supprimer/archiver le channel via le port
    drawChannelWriterPort.deleteById(channel.id());
  }
}

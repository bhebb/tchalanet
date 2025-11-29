package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.UpdateDrawChannelCommand;
import com.tchalanet.server.draw.application.port.in.command.UpdateDrawChannelCommandHandler;
import com.tchalanet.server.draw.application.port.out.DrawChannelReaderPort;
import com.tchalanet.server.draw.application.port.out.DrawChannelWriterPort;
import com.tchalanet.server.draw.domain.model.DrawChannel;
import com.tchalanet.server.draw.domain.model.DrawChannelId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateDrawChannelUseCase implements UpdateDrawChannelCommandHandler {

  private final DrawChannelReaderPort drawChannelReaderPort;
  private final DrawChannelWriterPort drawChannelWriterPort;

  @Override
  public DrawChannel handle(UpdateDrawChannelCommand command) {
    log.info("UpdateDrawChannelUseCase.handle - updating channel id={}", command.id());

    var existing =
        drawChannelReaderPort
            .findById(command.tenantId(), new DrawChannelId(command.id()))
            .orElseThrow(
                () -> new IllegalArgumentException("DrawChannel not found: " + command.id()));

    // appliquer les changements
    if (command.name() != null) existing.rename(command.name());
    if (command.active() != existing.isActive()) {
      if (command.active()) existing.activate();
      else existing.deactivate();
    }
    // code is probably not updatable, or handle if needed

    return drawChannelWriterPort.save(existing);
  }
}

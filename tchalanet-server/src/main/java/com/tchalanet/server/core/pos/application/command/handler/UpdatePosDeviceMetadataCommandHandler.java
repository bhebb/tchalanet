package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UpdatePosDeviceMetadataCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UpdatePosDeviceMetadataCommandHandler implements VoidCommandHandler<UpdatePosDeviceMetadataCommand> {

  @Override
  public void handle(UpdatePosDeviceMetadataCommand command) {
    // TODO: update metadata
    throw new UnsupportedOperationException("UpdatePosDeviceMetadataCommandHandler not implemented yet");
  }
}


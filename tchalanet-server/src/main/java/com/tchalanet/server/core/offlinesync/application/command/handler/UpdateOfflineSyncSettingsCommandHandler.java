package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.UpdateOfflineSyncSettingsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UpdateOfflineSyncSettingsCommandHandler implements VoidCommandHandler<UpdateOfflineSyncSettingsCommand> {

  @Override
  public void handle(UpdateOfflineSyncSettingsCommand command) {
    // TODO: update settings
    throw new UnsupportedOperationException("UpdateOfflineSyncSettingsCommandHandler not implemented yet");
  }
}


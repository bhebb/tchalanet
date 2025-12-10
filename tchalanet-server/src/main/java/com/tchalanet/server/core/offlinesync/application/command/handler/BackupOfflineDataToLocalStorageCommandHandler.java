package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.BackupOfflineDataToLocalStorageCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class BackupOfflineDataToLocalStorageCommandHandler implements VoidCommandHandler<BackupOfflineDataToLocalStorageCommand> {

  @Override
  public void handle(BackupOfflineDataToLocalStorageCommand command) {
    // TODO: backup offline queue to local storage
    throw new UnsupportedOperationException("BackupOfflineDataToLocalStorageCommandHandler not implemented yet");
  }
}


package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.ClearOfflineQueueAfterFullSyncCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ClearOfflineQueueAfterFullSyncCommandHandler
    implements VoidCommandHandler<ClearOfflineQueueAfterFullSyncCommand> {

  @Override
  public void handle(ClearOfflineQueueAfterFullSyncCommand command) {
    // TODO: clear queue after full sync
    throw new UnsupportedOperationException(
        "ClearOfflineQueueAfterFullSyncCommandHandler not implemented yet");
  }
}

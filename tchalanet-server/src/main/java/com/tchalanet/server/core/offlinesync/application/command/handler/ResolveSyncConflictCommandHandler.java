package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.ResolveSyncConflictCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ResolveSyncConflictCommandHandler
    implements VoidCommandHandler<ResolveSyncConflictCommand> {

  @Override
  public void handle(ResolveSyncConflictCommand command) {
    // TODO: apply resolution chosen by supervisor or automatic rule
    throw new UnsupportedOperationException(
        "ResolveSyncConflictCommandHandler not implemented yet");
  }
}

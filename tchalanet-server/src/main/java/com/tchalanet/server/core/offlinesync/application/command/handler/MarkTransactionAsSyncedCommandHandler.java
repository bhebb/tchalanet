package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.MarkTransactionAsSyncedCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class MarkTransactionAsSyncedCommandHandler implements VoidCommandHandler<MarkTransactionAsSyncedCommand> {

  @Override
  public void handle(MarkTransactionAsSyncedCommand command) {
    // TODO: mark transaction as synced in queue
    throw new UnsupportedOperationException("MarkTransactionAsSyncedCommandHandler not implemented yet");
  }
}


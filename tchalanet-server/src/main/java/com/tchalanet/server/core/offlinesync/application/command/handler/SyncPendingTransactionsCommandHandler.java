package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.SyncPendingTransactionsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class SyncPendingTransactionsCommandHandler implements VoidCommandHandler<SyncPendingTransactionsCommand> {

  @Override
  public void handle(SyncPendingTransactionsCommand command) {
    // TODO: sync pending transactions, detect conflicts
    throw new UnsupportedOperationException("SyncPendingTransactionsCommandHandler not implemented yet");
  }
}


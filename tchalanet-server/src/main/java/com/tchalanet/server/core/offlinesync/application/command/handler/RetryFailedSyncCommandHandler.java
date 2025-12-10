package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.RetryFailedSyncCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class RetryFailedSyncCommandHandler implements VoidCommandHandler<RetryFailedSyncCommand> {

  @Override
  public void handle(RetryFailedSyncCommand command) {
    // TODO: retry failed syncs
    throw new UnsupportedOperationException("RetryFailedSyncCommandHandler not implemented yet");
  }
}


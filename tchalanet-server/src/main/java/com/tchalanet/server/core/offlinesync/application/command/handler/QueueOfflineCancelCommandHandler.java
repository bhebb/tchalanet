package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.QueueOfflineCancelCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class QueueOfflineCancelCommandHandler implements VoidCommandHandler<QueueOfflineCancelCommand> {

  @Override
  public void handle(QueueOfflineCancelCommand command) {
    // TODO: enqueue cancel
    throw new UnsupportedOperationException("QueueOfflineCancelCommandHandler not implemented yet");
  }
}


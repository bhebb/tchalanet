package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.QueueOfflineSaleCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class QueueOfflineSaleCommandHandler implements VoidCommandHandler<QueueOfflineSaleCommand> {

  @Override
  public void handle(QueueOfflineSaleCommand command) {
    // TODO: enqueue sale for offline device
    throw new UnsupportedOperationException("QueueOfflineSaleCommandHandler not implemented yet");
  }
}

package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.QueueOfflinePayoutCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class QueueOfflinePayoutCommandHandler implements VoidCommandHandler<QueueOfflinePayoutCommand> {

  @Override
  public void handle(QueueOfflinePayoutCommand command) {
    // TODO: enqueue payout
    throw new UnsupportedOperationException("QueueOfflinePayoutCommandHandler not implemented yet");
  }
}


package com.tchalanet.server.core.offlinesync.internal.application.command.handler.batch;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.offlinesync.api.command.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.api.command.ReceiveOfflineBatchResult;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatchStatus;
import java.util.UUID;

public class ReceiveOfflineBatchCommandHandler
    implements CommandHandler<ReceiveOfflineBatchCommand, ReceiveOfflineBatchResult> {

  @Override
  public ReceiveOfflineBatchResult handle(ReceiveOfflineBatchCommand command) {
    return new ReceiveOfflineBatchResult(
        com.tchalanet.server.common.types.id.OfflineBatchId.of(UUID.randomUUID()),
        OfflineBatchStatus.RECEIVED,
        command.submissions() == null ? 0 : command.submissions().size(),
        0,
        0);
  }
}


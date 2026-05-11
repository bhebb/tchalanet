package com.tchalanet.server.core.offlinesync.application.command.handler.code;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.code.ExpireOfflineCodeBatchCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.code.ExpireOfflineCodeBatchResult;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExpireOfflineCodeBatchCommandHandler
    implements CommandHandler<ExpireOfflineCodeBatchCommand, ExpireOfflineCodeBatchResult> {

  @Override
  @TchTx
  public ExpireOfflineCodeBatchResult handle(ExpireOfflineCodeBatchCommand command) {
    return new ExpireOfflineCodeBatchResult(command.codeBatchId(), 0);
  }
}


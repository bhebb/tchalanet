package com.tchalanet.server.core.offlinesync.application.command.handler.batch;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.offlinesync.application.command.model.batch.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.batch.ProcessOfflineBatchWithSalesResult;

public class ProcessOfflineBatchWithSalesCommandHandler
    implements CommandHandler<ProcessOfflineBatchWithSalesCommand, ProcessOfflineBatchWithSalesResult> {

  @Override
  public ProcessOfflineBatchWithSalesResult handle(ProcessOfflineBatchWithSalesCommand command) {
    return new ProcessOfflineBatchWithSalesResult(command.batchId(), 0, 0, 0, 0);
  }
}


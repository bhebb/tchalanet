package com.tchalanet.server.core.offlinesync.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.application.port.out.SalesOfflineCommandPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.application.service.OfflineSalesPayloadMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ProcessOfflineBatchWithSalesCommandHandler
    implements CommandHandler<ProcessOfflineBatchWithSalesCommand, ProcessOfflineBatchWithSalesResult> {

  private final OfflineSubmissionReaderPort reader;
  private final OfflineBatchWriterPort batchWriter;
  private final SalesOfflineCommandPort salesOfflineCommandPort;
  private final OfflineSalesPayloadMapper payloadMapper;

  @Override
  @TchTx
  public ProcessOfflineBatchWithSalesResult handle(ProcessOfflineBatchWithSalesCommand cmd) {
    var submissions = reader.findReadyForSales(cmd.batchId());
    batchWriter.markSentToSales(cmd.batchId());

    var mappedSubmissions = payloadMapper.mapSubmissions(cmd.batchId(), submissions);
    salesOfflineCommandPort.syncBatch(cmd.batchId(), mappedSubmissions);

    return new ProcessOfflineBatchWithSalesResult(
        cmd.batchId(),
        0,
        0,
        0,
        0);
  }
}

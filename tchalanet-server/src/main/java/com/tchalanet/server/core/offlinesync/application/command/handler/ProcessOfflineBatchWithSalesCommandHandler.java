package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.sales.application.command.model.SyncOfflineSalesCommand;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ProcessOfflineBatchWithSalesCommandHandler
    implements CommandHandler<ProcessOfflineBatchWithSalesCommand, ProcessOfflineBatchWithSalesResult> {

  private final OfflineSubmissionReaderPort reader;
  private final OfflineSubmissionWriterPort writer;
  private final OfflineBatchWriterPort batchWriter;
  private final CommandBus commandBus;
  private final OfflineSalesPayloadMapper payloadMapper;

  @Override
  @TchTx
  public ProcessOfflineBatchWithSalesResult handle(ProcessOfflineBatchWithSalesCommand cmd) {
    var submissions = reader.findReadyForSales(cmd.batchId());
    batchWriter.markSentToSales(cmd.batchId());

    var salesCommand = payloadMapper.toSalesCommand(cmd.batchId(), submissions);
    var result = commandBus.execute(salesCommand);

    for (var decision : result.decisions()) {
      writer.recordSalesDecision(
          decision.submissionId(),
          decision.decision(),
          decision.rejectReason(),
          decision.ticketId());
    }

    return new ProcessOfflineBatchWithSalesResult(
        cmd.batchId(),
        result.acceptedCount(),
        result.rejectedCount(),
        result.reviewCount(),
        result.conflictCount());
  }
}

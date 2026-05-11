package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.offlinesync.application.command.model.ProcessOfflineBatchWithSalesCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.ReceiveOfflineBatchResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineCryptoPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineBatchStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReceiveOfflineBatchCommandHandler
    implements CommandHandler<ReceiveOfflineBatchCommand, ReceiveOfflineBatchResult> {

  private final OfflineCryptoPort crypto;
  private final OfflineBatchWriterPort batchWriter;
  private final CommandBus commandBus;

  @Override
  @TchTx
  public ReceiveOfflineBatchResult handle(ReceiveOfflineBatchCommand cmd) {
    int technicalRejects = 0;
    int ready = 0;

    for (var submission : cmd.submissions()) {
      boolean hashOk = crypto.verifyPayloadHash(submission.payloadJson(), submission.payloadHash());
      boolean signatureOk = crypto.verifyPayloadSignature(cmd.terminalId(), submission.payloadJson(), submission.payloadHash(), submission.signature());
      if (!hashOk || !signatureOk) {
        technicalRejects++;
      } else {
        ready++;
      }
    }

    var batchId = batchWriter.saveReceivedBatch(new Object());

    if (ready > 0) {
      AfterCommit.run(() -> commandBus.execute(new ProcessOfflineBatchWithSalesCommand(batchId)));
    }

    return new ReceiveOfflineBatchResult(
        batchId,
        ready > 0 ? OfflineBatchStatus.READY_FOR_SALES : OfflineBatchStatus.TECHNICALLY_REJECTED,
        cmd.submissions().size(),
        ready,
        technicalRejects);
  }
}

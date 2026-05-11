package com.tchalanet.server.core.offlinesync.application.command.handler.code;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeReservationId;
import com.tchalanet.server.core.offlinesync.application.command.model.code.IssueOfflineCodeBatchCommand;
import com.tchalanet.server.core.offlinesync.application.command.model.code.IssueOfflineCodeBatchResult;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineCodeBatchWriterPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineCodeReservation;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineCodeReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class IssueOfflineCodeBatchCommandHandler
    implements CommandHandler<IssueOfflineCodeBatchCommand, IssueOfflineCodeBatchResult> {

  private final OfflineCodeBatchWriterPort codeBatchWriterPort;

  @Override
  @TchTx
  public IssueOfflineCodeBatchResult handle(IssueOfflineCodeBatchCommand command) {
    var batchId = OfflineCodeBatchId.of(UUID.randomUUID());
    var issuedAt = Instant.now();
    var batch = new OfflineCodeBatch(
        batchId,
        command.tenantId(),
        command.terminalId(),
        command.requestedCount(),
        issuedAt,
        command.expiresAt());
    codeBatchWriterPort.save(batch);

    var codes = IntStream.range(0, command.requestedCount())
        .mapToObj(i -> "OFS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
        .toList();

    List<OfflineCodeReservation> reservations = codes.stream()
        .map(code -> new OfflineCodeReservation(
            OfflineCodeReservationId.of(UUID.randomUUID()),
            command.tenantId(),
            batchId,
            code,
            OfflineCodeReservationStatus.RESERVED,
            issuedAt,
            null))
        .toList();

    codeBatchWriterPort.saveReservations(reservations);
    return new IssueOfflineCodeBatchResult(batchId, codes);
  }
}


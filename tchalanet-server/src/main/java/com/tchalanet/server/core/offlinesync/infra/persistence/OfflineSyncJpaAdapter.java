package com.tchalanet.server.core.offlinesync.infra.persistence;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.domain.model.SalesOfflineRejectReason;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflineSyncJpaAdapter implements OfflineSubmissionReaderPort, OfflineSubmissionWriterPort, OfflineBatchWriterPort {

  private final OfflineSaleSubmissionJpaRepository submissionRepo;
  private final OfflineBatchJpaRepository batchRepo;
  private final OfflineSyncJpaMapper mapper;

  @Override
  public List<OfflineSaleSubmission> findReadyForSales(OfflineBatchId batchId) {
    return submissionRepo.findByBatchIdAndStatus(batchId.value(), "READY_FOR_SALES")
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public java.util.Optional<OfflineSaleSubmission> findById(OfflineSaleSubmissionId id) {
    return submissionRepo.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public void recordSalesDecision(
      OfflineSaleSubmissionId submissionId,
      SalesOfflineDecision decision,
      SalesOfflineRejectReason rejectReason,
      TicketId salesTicketId) {
    var e = submissionRepo.findById(submissionId.value()).orElseThrow();
    e.setSalesDecision(decision == null ? null : decision.name());
    e.setSalesRejectReason(rejectReason == null ? null : rejectReason.name());
    e.setSalesTicketId(salesTicketId == null ? null : salesTicketId.value());
    if (decision != null) {
      e.setStatus(switch (decision) {
        case ACCEPTED, ACCEPTED_POST_CLOSE_ADJUSTMENT -> "SALES_ACCEPTED";
        case REJECTED -> "SALES_REJECTED";
        case CONFLICT -> "SALES_CONFLICT";
        case REVIEW_REQUIRED -> "SALES_REVIEW_REQUIRED";
      });
    }
    submissionRepo.save(e);
  }

  @Override
  public OfflineBatchId saveReceivedBatch(Object batch) {
    // TODO map real aggregate/input.
    return OfflineBatchId.of(java.util.UUID.randomUUID());
  }

  @Override
  public void markSentToSales(OfflineBatchId batchId) {
    batchRepo.findById(batchId.value()).ifPresent(e -> {
      e.setStatus("SENT_TO_SALES");
      batchRepo.save(e);
    });
  }
}

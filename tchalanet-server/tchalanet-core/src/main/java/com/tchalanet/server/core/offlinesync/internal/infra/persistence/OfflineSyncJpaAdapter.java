package com.tchalanet.server.core.offlinesync.internal.infra.persistence;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineCodeBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSalesGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineCodeBatchWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantWriterPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionWriterPort;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineBatchStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineCodeBatch;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineCodeReservation;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineCodeReservationStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineRiskFlag;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrantStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineRejectReason;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfflineSyncJpaAdapter implements OfflineSubmissionReaderPort, OfflineSubmissionWriterPort, OfflineBatchWriterPort {

  private final OfflineSaleSubmissionJpaRepository submissionRepo;
  private final OfflineBatchJpaRepository batchRepo;
  private final OfflineSalesGrantJpaRepository grantRepo;
  private final OfflineCodeBatchJpaRepository codeBatchRepo;
  private final OfflineCodeReservationJpaRepository codeReservationRepo;
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
  public List<OfflineSaleSubmission> listByBatch(OfflineBatchId batchId, OfflineSubmissionStatus status) {
    var entities = status == null
        ? submissionRepo.findByBatchId(batchId.value())
        : submissionRepo.findByBatchIdAndStatus(batchId.value(), status.name());
    return entities.stream().map(mapper::toDomain).toList();
  }

  @Override
  public long countByTenantAndStatus(TenantId tenantId, OfflineSubmissionStatus status) {
    return submissionRepo.countByTenantIdAndStatus(tenantId.value(), status.name());
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
  public void markTechnicalReject(OfflineSaleSubmissionId submissionId) {
    var e = submissionRepo.findById(submissionId.value()).orElseThrow();
    e.setStatus(OfflineSubmissionStatus.TECHNICALLY_REJECTED.name());
    submissionRepo.save(e);
  }

  @Override
  public void updateRiskFlags(OfflineSaleSubmissionId submissionId, Set<OfflineRiskFlag> flags) {
    var e = submissionRepo.findById(submissionId.value()).orElseThrow();
    var json = flags == null || flags.isEmpty() ? "[]" :
        flags.stream().map(Enum::name).map(v -> "\"" + v + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"));
    e.setRiskFlagsJson(json);
    submissionRepo.save(e);
  }

  @Override
  public OfflineBatchId saveReceivedBatch(OfflineBatch batch) {
    return batch.id();
  }

  @Override
  public void markSentToSales(OfflineBatchId batchId) {
    batchRepo.findById(batchId.value()).ifPresent(e -> {
      e.setStatus("SENT_TO_SALES");
      batchRepo.save(e);
    });
  }

  public Optional<OfflineBatch> findById(OfflineBatchId id) {
    return batchRepo.findById(id.value()).map(this::batchToDomain);
  }

  private OfflineBatch batchToDomain(OfflineBatchJpaEntity e) {
    return new OfflineBatch(
        OfflineBatchId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        com.tchalanet.server.common.types.id.TerminalId.of(e.getTerminalId()),
        com.tchalanet.server.common.types.id.OfflineSalesGrantId.of(e.getGrantId()),
        OfflineCodeBatchId.of(e.getCodeBatchId()),
        e.getClientBatchId(),
        e.getReceivedAt(),
        OfflineBatchStatus.valueOf(e.getStatus()),
        e.getTicketCount(),
        e.getTechnicalRejectCount(),
        e.getSalesAcceptCount(),
        e.getSalesRejectCount(),
        e.getReviewCount());
  }

  public Optional<OfflineSalesGrant> findById(OfflineSalesGrantId id) {
    return grantRepo.findById(id.value()).map(e -> new OfflineSalesGrant(
        OfflineSalesGrantId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        com.tchalanet.server.common.types.id.TerminalId.of(e.getTerminalId()),
        com.tchalanet.server.common.types.id.OutletId.of(e.getOutletId()),
        com.tchalanet.server.common.types.id.SalesSessionId.of(e.getSalesSessionId()),
        com.tchalanet.server.common.types.id.UserId.of(e.getSellerUserId()),
        OfflineCodeBatchId.of(e.getCodeBatchId()),
        OfflineSalesGrantStatus.valueOf(e.getStatus()),
        e.getIssuedAt(),
        e.getExpiresAt()));
  }

  public OfflineSalesGrantId save(OfflineSalesGrant grant) {
    var entity = new OfflineSalesGrantJpaEntity();
    entity.setId(grant.id().value());
    entity.setTenantId(grant.tenantId().value());
    entity.setTerminalId(grant.terminalId().value());
    entity.setOutletId(grant.outletId().value());
    entity.setSellerUserId(grant.sellerUserId().value());
    entity.setSalesSessionId(grant.salesSessionId().value());
    entity.setCodeBatchId(grant.codeBatchId().value());
    entity.setStatus(grant.status().name());
    entity.setIssuedAt(grant.issuedAt());
    entity.setExpiresAt(grant.expiresAt());
    grantRepo.save(entity);
    return grant.id();
  }

  public void updateStatus(OfflineSalesGrantId grantId, OfflineSalesGrantStatus status) {
    grantRepo.findById(grantId.value()).ifPresent(entity -> {
      entity.setStatus(status.name());
      grantRepo.save(entity);
    });
  }

  public Optional<OfflineCodeBatch> findById(OfflineCodeBatchId id) {
    return codeBatchRepo.findById(id.value()).map(e -> new OfflineCodeBatch(
        OfflineCodeBatchId.of(e.getId()),
        TenantId.of(e.getTenantId()),
        com.tchalanet.server.common.types.id.TerminalId.of(e.getTerminalId()),
        e.getAllocatedCount(),
        e.getIssuedAt(),
        e.getExpiresAt()));
  }

  public OfflineCodeBatchId save(OfflineCodeBatch batch) {
    var entity = new OfflineCodeBatchJpaEntity();
    entity.setId(batch.id().value());
    entity.setTenantId(batch.tenantId().value());
    entity.setTerminalId(batch.terminalId().value());
    entity.setAllocatedCount(batch.allocatedCount());
    entity.setIssuedAt(batch.issuedAt());
    entity.setExpiresAt(batch.expiresAt());
    codeBatchRepo.save(entity);
    return batch.id();
  }

  public void saveReservations(List<OfflineCodeReservation> reservations) {
    var entities = reservations.stream().map(r -> {
      var e = new OfflineCodeReservationJpaEntity();
      e.setId(r.id().value());
      e.setTenantId(r.tenantId().value());
      e.setCodeBatchId(r.codeBatchId().value());
      e.setOfflineCode(r.offlineCode());
      e.setStatus(r.status().name());
      e.setReservedAt(r.reservedAt());
      e.setConsumedAt(r.consumedAt());
      return e;
    }).toList();
    codeReservationRepo.saveAll(entities);
  }
}

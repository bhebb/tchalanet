package com.tchalanet.server.platform.archive.internal.service;

import com.tchalanet.server.platform.archive.api.model.ArchiveLegalHoldRowView;
import com.tchalanet.server.platform.archive.api.model.ArchiveObjectRowView;
import com.tchalanet.server.platform.archive.api.model.ArchiveOpsSummaryView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunRowView;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveLegalHoldJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveObjectJdbcRepository;
import com.tchalanet.server.platform.archive.internal.persistence.ArchiveRunJdbcRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArchiveOpsQueryService {

  private final ArchiveLegalHoldJdbcRepository legalHolds;
  private final ArchiveRunJdbcRepository runs;
  private final ArchiveObjectJdbcRepository objects;

  public UUID createLegalHold(
      UUID tenantId,
      String datasetCode,
      String entityType,
      String entityId,
      LocalDate periodStart,
      LocalDate periodEnd,
      String reason,
      UUID requestedBy) {
    return legalHolds.create(
        tenantId, datasetCode, entityType, entityId, periodStart, periodEnd, reason, requestedBy);
  }

  public void releaseLegalHold(UUID holdId, UUID releasedBy, String reason) {
    legalHolds.release(holdId, releasedBy, reason);
  }

  public List<ArchiveLegalHoldRowView> listActiveLegalHolds(int limit) {
    return legalHolds.listActive(limit);
  }

  public List<ArchiveRunRowView> listFailedRuns(int limit) {
    return runs.listFailed(limit);
  }

  public List<ArchiveObjectRowView> listInvalidObjects(int limit) {
    return objects.listInvalid(limit);
  }

  public ArchiveOpsSummaryView summary() {
    return new ArchiveOpsSummaryView(
        runs.countByStatus("FAILED"),
        runs.countByStatus("STARTED"),
        runs.countByStatus("COMPLETED"),
        objects.countByStatus("INVALID"),
        objects.countByStatus("VERIFIED"),
        objects.countByStatus("PENDING"));
  }
}

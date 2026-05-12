package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineBatchId;
import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSaleSubmission;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineDecision;
import com.tchalanet.server.core.offlinesync.internal.domain.model.SalesOfflineRejectReason;
import java.util.List;

public interface SalesOfflineCommandPort {
  SyncResult syncBatch(OfflineBatchId batchId, List<OfflineSaleSubmission> submissions);

  record SyncResult(
      int acceptedCount,
      int rejectedCount,
      int reviewCount,
      int conflictCount,
      List<Decision> decisions
  ) {}

  record Decision(
      OfflineSaleSubmissionId submissionId,
      SalesOfflineDecision decision,
      SalesOfflineRejectReason rejectReason,
      TicketId ticketId
  ) {}
}


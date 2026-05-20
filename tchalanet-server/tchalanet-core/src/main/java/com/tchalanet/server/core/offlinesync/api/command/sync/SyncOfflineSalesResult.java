package com.tchalanet.server.core.offlinesync.api.command.sync;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.OfflineSyncBatchId;

import java.util.List;

public record SyncOfflineSalesResult(
    OfflineSyncBatchId syncBatchId,
    List<SubmissionOutcome> outcomes
) {

    public enum Outcome { ACCEPTED, REJECTED, DUPLICATE }

    /** Per-submission outcome returned to the device. */
    public record SubmissionOutcome(
        String clientSubmissionId,
        OfflineSubmissionId submissionId,
        Outcome outcome,
        String rejectionCode,
        String rejectionReason
    ) {}
}

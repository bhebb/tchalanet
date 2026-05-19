package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionSyncWindowWriterPort;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionJpaRepository;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.SyncAcceptedWindowQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class OfflineSubmissionSyncWindowJpaAdapter implements OfflineSubmissionSyncWindowWriterPort {

    private static final String SYNC_WINDOW_CLOSED_CODE = "offlinesync.grant.sync_window_closed";
    private static final String SYNC_WINDOW_CLOSED_REASON =
        "Grant sync window closed before submission was processed";

    private final SyncAcceptedWindowQueryRepository query;
    private final OfflineSubmissionJpaRepository submissionRepo;

    @Override
    public int closeWindowForTenant(Instant now) {
        var orphans = query.findReceivedAfterWindowClosed(now);
        if (orphans.isEmpty()) {
            return 0;
        }

        for (var submission : orphans) {
            submission.setStatus(OfflineSubmissionStatus.SYNC_FAILED.name());
            submission.setRejectionCode(SYNC_WINDOW_CLOSED_CODE);
            submission.setRejectionReason(SYNC_WINDOW_CLOSED_REASON);
            submission.setProcessedAt(now);
        }
        submissionRepo.saveAll(orphans);
        return orphans.size();
    }
}


package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;

import java.util.List;

/**
 * Persistence port for {@code offline_submission_line} rows. Lines are stored alongside
 * each accepted submission so the recover-stuck workflow can rebuild a {@code TicketDraft}
 * without holding the original device payload.
 */
public interface OfflineSubmissionLineWriterPort {

    void saveAll(TenantId tenantId, OfflineSubmissionId submissionId,
                 List<OfflineSubmissionLineSnapshot> lines);
}

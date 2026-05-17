package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSubmission;
import java.util.List;

public interface OfflineSubmissionWriterPort {
    OfflineSubmission save(OfflineSubmission submission);
    List<OfflineSubmission> saveAll(List<OfflineSubmission> submissions);
    int claimForProcessing(List<OfflineSubmission> submissions);
}

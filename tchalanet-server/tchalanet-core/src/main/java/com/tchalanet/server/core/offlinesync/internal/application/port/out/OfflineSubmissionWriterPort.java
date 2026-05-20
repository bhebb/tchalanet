package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.core.offlinesync.internal.domain.model.submission.OfflineSubmission;

public interface OfflineSubmissionWriterPort {

    OfflineSubmission save(OfflineSubmission submission);
}

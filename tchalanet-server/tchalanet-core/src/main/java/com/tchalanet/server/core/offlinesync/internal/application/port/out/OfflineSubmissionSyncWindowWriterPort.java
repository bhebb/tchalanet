package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import java.time.Instant;

public interface OfflineSubmissionSyncWindowWriterPort {

    int closeWindowForTenant(Instant now);
}


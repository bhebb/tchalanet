package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;

public record ExpireOfflineCodeBatchResult(OfflineCodeBatchId codeBatchId, int expiredCount) {}


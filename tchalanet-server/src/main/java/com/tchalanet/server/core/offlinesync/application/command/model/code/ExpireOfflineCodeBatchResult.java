package com.tchalanet.server.core.offlinesync.application.command.model.code;

import com.tchalanet.server.common.types.id.OfflineCodeBatchId;

public record ExpireOfflineCodeBatchResult(OfflineCodeBatchId codeBatchId, int expiredCount) {}


package com.tchalanet.server.core.offlinesync.api.command;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;

public record RejectOfflineSubmissionResult(OfflineSaleSubmissionId submissionId, String status) {}


package com.tchalanet.server.core.offlinesync.application.command.model.review;

import com.tchalanet.server.common.types.id.OfflineSaleSubmissionId;

public record RejectOfflineSubmissionResult(OfflineSaleSubmissionId submissionId, String status) {}


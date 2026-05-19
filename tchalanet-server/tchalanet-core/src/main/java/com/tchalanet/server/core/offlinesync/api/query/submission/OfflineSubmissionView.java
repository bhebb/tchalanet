package com.tchalanet.server.core.offlinesync.api.query.submission;

import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;

import java.time.Instant;

public record OfflineSubmissionView(
    OfflineSubmissionId id,
    OfflineGrantId grantId,
    UserId sellerUserId,
    OfflineSubmissionStatus status,
    Instant clientSoldAt,
    Instant receivedAt,
    Instant processedAt,
    Money totalStakeAmount,
    int lineCount,
    String rejectionCode,
    String rejectionReason,
    TicketId createdTicketId
) {}

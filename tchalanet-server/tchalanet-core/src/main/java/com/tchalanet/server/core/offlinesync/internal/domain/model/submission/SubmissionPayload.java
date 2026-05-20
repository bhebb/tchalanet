package com.tchalanet.server.core.offlinesync.internal.domain.model.submission;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.money.Money;

import java.time.Instant;

/**
 * Business payload of an offline sale.
 *
 * @param drawId The draw the device pinned at sale time. Persisted so the recover-stuck
 *               handler can rebuild the {@code TicketDraft} without holding the original
 *               device payload.
 */
public record SubmissionPayload(
    DrawId drawId,
    Instant clientSoldAt,
    Money totalStakeAmount,
    int lineCount,
    String payloadHash,
    String signature
) {
    public SubmissionPayload {
        if (drawId == null) throw new IllegalArgumentException("drawId required");
        if (clientSoldAt == null) throw new IllegalArgumentException("clientSoldAt required");
        if (totalStakeAmount == null) throw new IllegalArgumentException("totalStakeAmount required");
        if (lineCount <= 0) throw new IllegalArgumentException("lineCount must be positive");
        if (payloadHash == null || payloadHash.isBlank())
            throw new IllegalArgumentException("payloadHash required");
    }
}

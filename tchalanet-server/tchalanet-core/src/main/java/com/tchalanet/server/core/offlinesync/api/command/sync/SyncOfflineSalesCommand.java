package com.tchalanet.server.core.offlinesync.api.command.sync;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * POS uploads a batch of offline submissions for technical validation.
 *
 * @param trustedOperationalContext true iff the calling request carried a strongly-trusted
 *                                  operational context (set by the controller from the
 *                                  {@code TchRequestContext.operationalContext()} trust level).
 */
public record SyncOfflineSalesCommand(
    @NotNull TenantId tenantId,
    @NotNull OfflineGrantId grantId,
    @NotBlank String clientBatchId,
    @NotBlank String batchPayloadHash,
    @NotEmpty List<Submission> submissions,
    boolean trustedOperationalContext
) implements Command<SyncOfflineSalesResult> {

    /** Backwards-compatible constructor for callers (e.g. tests) without the trust flag. */
    public SyncOfflineSalesCommand(TenantId tenantId, OfflineGrantId grantId,
                                   String clientBatchId, String batchPayloadHash,
                                   List<Submission> submissions) {
        this(tenantId, grantId, clientBatchId, batchPayloadHash, submissions, false);
    }

    /**
     * Individual submission payload as uploaded by the device.
     *
     * @param drawId the draw this offline sale is for — pinned by the cashier at sale time.
     */
    public record Submission(
        @NotBlank String clientSubmissionId,
        @NotBlank String offlineCode,
        @NotNull DrawId drawId,
        @NotNull Instant clientSoldAt,
        @NotNull Money totalStakeAmount,
        @NotEmpty List<Line> lines,
        @NotBlank String payloadHash,
        @NotBlank String signature
    ) {
        public int lineCount() { return lines == null ? 0 : lines.size(); }
    }

    /** A single line of a submitted sale, mirrored into the promotion event for sales. */
    public record Line(
        int lineNo,
        @NotBlank String gameCode,
        @NotBlank String betType,
        @NotBlank String betOption,
        @NotBlank String selectionKey,
        @NotNull Money stakeAmount,
        Money potentialPayout
    ) {}
}

package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineGrantId;
import com.tchalanet.server.common.types.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record SyncOfflineSalesRequest(
    @NotNull OfflineGrantId grantId,
    @NotBlank String clientBatchId,
    @NotBlank String batchPayloadHash,
    @NotEmpty @Valid List<Submission> submissions
) {

    public record Submission(
        @NotBlank String clientSubmissionId,
        @NotBlank String offlineCode,
        @NotNull DrawId drawId,
        @NotNull Instant clientSoldAt,
        @NotNull Money totalStakeAmount,
        @NotEmpty @Valid List<Line> lines,
        @NotBlank String payloadHash,
        @NotBlank String signature
    ) {}

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

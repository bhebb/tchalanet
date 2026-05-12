package com.tchalanet.server.core.offlinesync.api.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record OfflineSaleSubmissionInput(
    @NotBlank String offlineCode,
    @NotBlank String clientTicketId,
    long localSequence,
    @NotNull Instant createdAtDevice,
    @NotBlank String payloadJson,
    @NotBlank String payloadHash,
    @NotBlank String signature
) {}

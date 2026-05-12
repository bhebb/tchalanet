package com.tchalanet.server.core.offlinesync.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record OfflineSubmissionRequest(
    @NotBlank String offlineCode,
    @NotBlank String clientTicketId,
    long localSequence,
    @NotNull Instant createdAtDevice,
    @NotBlank String payloadJson,
    @NotBlank String payloadHash,
    @NotBlank String signature
) {}

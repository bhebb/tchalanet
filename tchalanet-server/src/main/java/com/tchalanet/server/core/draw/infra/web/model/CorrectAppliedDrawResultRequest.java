package com.tchalanet.server.core.draw.infra.web.model;

import com.tchalanet.server.common.types.id.DrawResultId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CorrectAppliedDrawResultRequest(
    @NotNull DrawResultId correctedDrawResultId,
    @NotBlank String reason,
    @NotBlank String idempotencyKey,
    boolean force
) {}

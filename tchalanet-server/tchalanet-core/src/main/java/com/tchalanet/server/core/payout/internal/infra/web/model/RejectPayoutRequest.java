package com.tchalanet.server.core.payout.internal.infra.web.model;

import jakarta.validation.constraints.NotBlank;

public record RejectPayoutRequest(
    @NotBlank String reason
) {}

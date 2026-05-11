package com.tchalanet.server.core.payout.infra.web.model;

import jakarta.validation.constraints.NotBlank;

public record RejectPayoutRequest(
    @NotBlank String reason
) {}

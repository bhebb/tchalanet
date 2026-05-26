package com.tchalanet.server.core.terminal.internal.infra.web.tenant.model;

import jakarta.validation.constraints.NotBlank;

public record VerifyTerminalActivationChallengeRequest(
    @NotBlank String clearCode,
    @NotBlank String bindingCredential,
    String bindingPublicKey,
    String deviceFingerprintHash
) {}

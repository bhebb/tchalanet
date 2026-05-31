package com.tchalanet.server.core.terminal.internal.infra.web.tenant.model;

import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalPublicKeyAlgorithm;
import jakarta.validation.constraints.NotBlank;

public record VerifyTerminalActivationChallengeRequest(
    @NotBlank String clearCode,
    @NotBlank String bindingCredential,
    String bindingPublicKey,
    TerminalPublicKeyAlgorithm publicKeyAlgorithm,
    String deviceFingerprintHash
) {}

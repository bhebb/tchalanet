package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalPublicKeyAlgorithm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyTerminalActivationChallengeCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull TerminalActivationChallengeId challengeId,
    @NotNull UserId userId,
    @NotBlank String clearCode,
    @NotNull TerminalBindingType bindingType,
    String bindingPublicKey,
    TerminalPublicKeyAlgorithm publicKeyAlgorithm,
    String bindingCredential,
    String deviceFingerprintHash,
    @NotNull UserId actorUserId
) implements Command<VerifyTerminalActivationChallengeResult> {}

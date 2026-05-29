package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryMode;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeType;
import jakarta.validation.constraints.NotNull;

public record CreateTerminalActivationChallengeCommand(
    @NotNull TenantId tenantId,
    @NotNull TerminalId terminalId,
    @NotNull UserId userId,
    @NotNull TerminalChallengeType challengeType,
    @NotNull TerminalChallengeDeliveryMode deliveryMode,
    @NotNull UserId actorUserId
) implements Command<CreateTerminalActivationChallengeResult> {}

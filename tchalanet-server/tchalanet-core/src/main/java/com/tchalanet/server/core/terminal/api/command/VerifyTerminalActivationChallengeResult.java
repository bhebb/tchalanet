package com.tchalanet.server.core.terminal.api.command;

import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingType;

public record VerifyTerminalActivationChallengeResult(
    TerminalId terminalId,
    TerminalBindingId bindingId,
    TerminalBindingType bindingType
) {}

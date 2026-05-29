package com.tchalanet.server.core.terminal.internal.infra.web.tenant.model;

import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalChallengeDeliveryMode;

public record CreateTerminalActivationChallengeRequest(
    TerminalChallengeDeliveryMode deliveryMode
) {}

package com.tchalanet.server.core.session.application.command.model;

import java.util.UUID;

public record RefreshSessionTokenCommand(
    UUID tenantId,
    UUID sessionId,
    String refreshToken
) {}


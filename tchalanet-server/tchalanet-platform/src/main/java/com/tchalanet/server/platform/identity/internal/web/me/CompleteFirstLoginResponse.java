package com.tchalanet.server.platform.identity.internal.web.me;

import com.tchalanet.server.common.types.id.UserId;

public record CompleteFirstLoginResponse(
    UserId userId,
    boolean mustChangePassword,
    boolean mustCompleteProfile,
    String entryRoute) {}
